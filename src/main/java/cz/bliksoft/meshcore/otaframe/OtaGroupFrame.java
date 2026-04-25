package cz.bliksoft.meshcore.otaframe;

import java.util.Collection;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshCoreCrypto;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Group encrypted payload: GRP_TXT, GRP_DATA.
 *
 * <pre>
 *   channel_hash(1)  MAC(2)  ciphertext(N*16)
 * </pre>
 *
 * <p>
 * After a successful call to {@link #tryDecrypt} the fields
 * {@link #decryptedChannelName}, {@link #decryptedKey},
 * {@link #decryptedTimestamp}, and {@link #decryptedText} are populated.
 */
public class OtaGroupFrame extends OtaFrame {

	/** First byte of SHA-256(channel AES key) — used to identify the channel. */
	public final int channelHash;
	/** 2-byte MAC followed by AES-128-ECB ciphertext. */
	public final byte[] macAndCipher;

	/**
	 * Channel name (e.g. {@code "#public"}), or {@code null} if not yet decrypted.
	 */
	public String decryptedChannelName = null;
	/** 16-byte AES-128 channel key, or {@code null} if not yet decrypted. */
	public byte[] decryptedKey = null;
	/** Unix epoch seconds of the decrypted message, or -1 if not yet decrypted. */
	public long decryptedTimestamp = -1;
	/** Plaintext message, or {@code null} if not yet decrypted. */
	public String decryptedText = null;

	OtaGroupFrame(MeshcoreCompanion source, OtaRouteType route, OtaPayloadType payloadType, int ver, int tc0, int tc1,
			int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
		ByteReader br = new ByteReader(payloadBytes);
		channelHash = br.remaining() >= 1 ? br.readUnsignedByte() : -1;
		macAndCipher = br.remaining() > 0 ? br.readBytes() : new byte[0];
	}

	/**
	 * Try to decrypt this group payload using the supplied known channels. The
	 * 1-byte channel hash pre-filter inside
	 * {@link MeshCoreCrypto#tryDecryptWithKey} makes mismatches cheap. On success
	 * the decrypted data is stored in {@link #decryptedChannelName},
	 * {@link #decryptedKey}, {@link #decryptedTimestamp}, and
	 * {@link #decryptedText}.
	 *
	 * @param channels known channels (from companion)
	 * @return true if a channel matched and decryption succeeded
	 */
	public boolean tryDecrypt(Collection<ChannelInfo> channels) {
		if (channelHash < 0 || macAndCipher.length < 18)
			return false;
		for (ChannelInfo ch : channels) {
			MeshCoreCrypto.GrpTxtResult result = MeshCoreCrypto.tryDecryptWithKey(payloadBytes, ch.getPubkey());
			if (result != null) {
				decryptedChannelName = ch.getName();
				decryptedKey = ch.getPubkey();
				decryptedTimestamp = result.timestamp;
				decryptedText = result.text;
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		if (channelHash < 0)
			return routingPrefix() + " <incomplete>";
		int encLen = macAndCipher.length - 2;
		return String.format("%s ch=%02x enc=%dB data=%s", routingPrefix(), channelHash, encLen,
				decryptedText == null ? MeshcoreUtils.hex(payloadBytes)
						: String.format("[%s]%s", decryptedChannelName, decryptedText));
	}
}
