package cz.bliksoft.meshcore.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;

/**
 * Installs a {@link LogRXDataPush} listener that automatically decrypts
 * incoming GRP_TXT OTA packets.
 *
 * <p>
 * Three-stage lookup on every packet:
 * <ol>
 * <li>Known channels — from {@link MeshcoreCompanion#getChannels()}</li>
 * <li>Previously discovered channels — from past brute-force runs</li>
 * <li>Background brute-force — started at most once per channel hash</li>
 * </ol>
 */
public class GroupTextDecryptor {

	public interface Listener {
		/**
		 * Called when a GRP_TXT packet is successfully decrypted.
		 *
		 * @param channelName channel name including '#' prefix
		 * @param key         16-byte AES key
		 * @param timestamp   Unix epoch seconds from the decrypted plaintext
		 * @param text        decrypted message text
		 * @param snr         signal-to-noise ratio in dB
		 * @param rssi        received signal strength in dBm
		 */
		void onDecrypted(String channelName, byte[] key, long timestamp, String text, float snr, int rssi);

		/**
		 * Called when brute-force discovers a previously unknown channel. After this
		 * the channel is cached so future packets decrypt instantly.
		 *
		 * @param channelName discovered channel name including '#' prefix
		 * @param key         16-byte AES key
		 */
		void onChannelDiscovered(String channelName, byte[] key);

		/**
		 * Called when a brute-force run finishes — either all combinations were
		 * exhausted or it was stopped early via
		 * {@link GroupTextDecryptor#stopBruteForce}.
		 *
		 * @param channelHash the 1-byte channel hash that was searched
		 * @param matched     number of candidate matches found (0 = not found)
		 * @param stopped     true if terminated early, false if fully exhausted
		 */
		default void onBruteForceComplete(byte channelHash, long matched, boolean stopped) {
		}
	}

	/** channelName (with '#') → 16-byte AES key, populated by brute-force. */
	private final Map<String, byte[]> discoveredChannels = new ConcurrentHashMap<>();

	/**
	 * Channel hash → stop signal for each running (or completed) brute-force. Used
	 * both to avoid duplicate searches and to allow early termination.
	 */
	private final Map<Byte, AtomicBoolean> bruteForceSignals = new ConcurrentHashMap<>();

	private final int bruteForceMinLen;
	private final int bruteForceMaxLen;

	public GroupTextDecryptor(int bruteForceMinLen, int bruteForceMaxLen) {
		this.bruteForceMinLen = bruteForceMinLen;
		this.bruteForceMaxLen = bruteForceMaxLen;
	}

	/**
	 * Pre-populates the discovered cache with a channel whose name is already
	 * known. Packets from this channel will be decrypted in stage 2 rather than
	 * waiting for brute-force.
	 */
	public void addKnownChannel(String channelName, byte[] key) {
		discoveredChannels.put(channelName, key);
	}

	/**
	 * Stops any running brute-force search for the given channel hash byte. The
	 * background thread will finish its current work unit and then call
	 * {@link Listener#onBruteForceComplete} with {@code stopped=true}.
	 */
	public void stopBruteForce(byte channelHash) {
		AtomicBoolean signal = bruteForceSignals.get(channelHash);
		if (signal != null)
			signal.set(true);
	}

	/**
	 * Stops all running brute-force searches.
	 */
	public void stopAllBruteForce() {
		bruteForceSignals.values().forEach(s -> s.set(true));
	}

	/**
	 * Registers a {@link LogRXDataPush} listener on {@code companion} that
	 * automatically decrypts incoming GRP_TXT packets.
	 */
	public void install(MeshcoreCompanion companion, Listener listener) {
		companion.registerFrameListener(LogRXDataPush.class, frame -> {
			byte[] grpPayload = extractGrpTxtPayload(frame.getRawData());
			if (grpPayload == null)
				return;

			float snr = frame.getSnr4() / 4.0f;
			int rssi = frame.getRssi();

			// Stage 1: channels configured in the companion
			for (ChannelInfo ch : companion.getChannels()) {
				if (ChannelBruteForce.tryDecryptWithKey(grpPayload, ch.getName(), ch.getPubkey(),
						(name, key, ts, text) -> listener.onDecrypted(name, key, ts, text, snr, rssi)))
					return;
			}

			// Stage 2: channels discovered by previous brute-force runs
			for (Map.Entry<String, byte[]> e : discoveredChannels.entrySet()) {
				if (ChannelBruteForce.tryDecryptWithKey(grpPayload, e.getKey(), e.getValue(),
						(name, key, ts, text) -> listener.onDecrypted(name, key, ts, text, snr, rssi)))
					return;
			}

			// Stage 3: background brute-force, at most once per channel hash
			byte channelHash = grpPayload[0];
			AtomicBoolean stopSignal = new AtomicBoolean(false);
			if (bruteForceSignals.putIfAbsent(channelHash, stopSignal) != null)
				return;

			final byte[] payload = grpPayload;
			Thread t = new Thread(() -> {
				try {
					ChannelBruteForce.bruteForce(payload, bruteForceMinLen, bruteForceMaxLen,
							new ChannelBruteForce.MatchCallback() {
								@Override
								public void onMatch(String name, byte[] key, long ts, String text) {
									discoveredChannels.put(name, key);
									listener.onChannelDiscovered(name, key);
									listener.onDecrypted(name, key, ts, text, snr, rssi);
								}

								@Override
								public void onComplete(long matched, boolean stopped) {
									listener.onBruteForceComplete(channelHash, matched, stopped);
								}
							}, stopSignal);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}, String.format("grp-brute-%02x", channelHash & 0xFF));
			t.setDaemon(true);
			t.start();
		});
	}

	/**
	 * Parses a raw OTA packet from {@link LogRXDataPush#getRawData()} and returns
	 * the GRP_TXT application payload, or {@code null} if the packet is not a
	 * GRP_TXT or is too short.
	 */
	private static byte[] extractGrpTxtPayload(byte[] raw) {
		if (raw == null || raw.length < 3)
			return null;
		int i = 0;
		int header = raw[i++] & 0xFF;
		OtaConstants.OtaRouteType route = OtaConstants.OtaRouteType.fromByte((byte) (header & 0x03));
		OtaConstants.OtaPayloadType type = OtaConstants.OtaPayloadType.fromByte((byte) ((header >> 2) & 0x0F));
		if (type != OtaConstants.OtaPayloadType.GRP_TXT)
			return null;

		if (route.hasTransportCodes())
			i += 4; // skip two uint16 transport codes
		if (i >= raw.length)
			return null;

		int pathLenEncoded = raw[i++] & 0xFF;
		int hashSize = (pathLenEncoded >> 6) + 1;
		int hopCount = pathLenEncoded & 0x3F;
		i += hopCount * hashSize;
		if (i >= raw.length)
			return null;

		byte[] payload = Arrays.copyOfRange(raw, i, raw.length);
		return payload.length >= 1 + 2 + 16 ? payload : null;
	}
}
