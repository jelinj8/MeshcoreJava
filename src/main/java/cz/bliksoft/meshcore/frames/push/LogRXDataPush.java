package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;
import cz.bliksoft.meshcore.otaframe.OtaFrame;
import cz.bliksoft.meshcore.otaframe.OtaGroupFrame;
import cz.bliksoft.meshcore.otaframe.OtaUnicastFrame;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame logged by the device for every received over-the-air packet,
 * carrying SNR, RSSI, and the raw packet bytes so that the host can inspect or
 * decode all radio traffic.
 */
public class LogRXDataPush extends ResponseFrame {

	final int snr4;

	/**
	 * @return SNR of the received packet as a scaled integer; divide by 4.0 to get
	 *         dB
	 */
	public int getSnr4() {
		return snr4;
	}

	/**
	 * @return RSSI of the received packet in dBm
	 */
	public int getRssi() {
		return rssi;
	}

	/**
	 * @return raw over-the-air packet bytes, unparsed
	 */
	public byte[] getRawData() {
		return rawData;
	}

	final int rssi;
	final byte[] rawData;

	private OtaFrame cachedOtaFrame;

	public LogRXDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		snr4 = br.readSignedByte();
		rssi = br.readSignedByte();
		rawData = br.readBytes();
	}

	/**
	 * verbose path info - try to identify repeaters
	 */
	public static boolean isTranslatePath() {
		return translatePath;
	}

	/**
	 * verbose path info - try to identify repeaters
	 */
	public static void setTranslatePath(boolean translatePath) {
		LogRXDataPush.translatePath = translatePath;
	}

	/**
	 * parse raw data to basic frame info
	 */
	public static boolean isDecodeRaw() {
		return decodeRaw;
	}

	/**
	 * parse raw data to basic frame info
	 */
	public static void setDecodeRaw(boolean decodeRaw) {
		LogRXDataPush.decodeRaw = decodeRaw;
	}

	/**
	 * parse whole payload, including decription where possible
	 */
	public static boolean isDecodePaylodad() {
		return decodePaylodad;
	}

	/**
	 * parse whole payload, including decription where possible
	 */
	public static void setDecodePaylodad(boolean decodePaylodad) {
		LogRXDataPush.decodePaylodad = decodePaylodad;
	}

	/**
	 * verbose path info - try to identify repeaters
	 */
	private static boolean translatePath = false;

	/**
	 * parse raw data to basic frame info
	 */
	private static boolean decodeRaw = true;

	/**
	 * parse whole payload, including decription where possible
	 */
	private static boolean decodePaylodad = false;

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_LOG_RX_DATA;
	}

	/**
	 * Parse the raw OTA packet and return the typed {@link OtaFrame}. The result is
	 * cached after the first call.
	 *
	 * @return parsed frame, or {@code null} if the raw data is unparseable
	 */
	public OtaFrame getOtaFrame() {
		if (cachedOtaFrame == null)
			cachedOtaFrame = OtaFrame.parse(companion, rawData);
		return cachedOtaFrame;
	}

	@Override
	public String toString() {
		if (decodeRaw) {
			OtaFrame frame = getOtaFrame();
			String decoded;
			if (frame instanceof OtaUnicastFrame) {
				if (decodePaylodad)
					tryDecryptTxtMsg(companion.getConfig().getPrivateKey());
				decoded = frame.toString();
			} else if (frame instanceof OtaGroupFrame) {
				if (decodePaylodad)
					tryDecryptGrpTxt();
				decoded = frame.toString();
			} else {
				decoded = frame != null ? frame.toString() : "<unparseable>";
			}
			return String.format("%s snr:%.2f rssi:%d %s", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi,
					decoded);
		} else {
			return String.format("%s snr:%.2f rssi:%d %s", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi,
					MeshcoreUtils.hex(rawData));

		}
	}

	// ---- decryption with known keys ----

	/**
	 * Try to decrypt a GRP_TXT payload in this packet using the channels known to
	 * the companion. On success the decrypted data is stored in the underlying
	 * {@link OtaGroupFrame} fields ({@code decryptedChannelName},
	 * {@code decryptedKey}, {@code decryptedTimestamp}, {@code decryptedText}).
	 *
	 * @return true if a channel matched and decryption succeeded
	 */
	public boolean tryDecryptGrpTxt() {
		OtaFrame frame = getOtaFrame();
		if (!(frame instanceof OtaGroupFrame))
			return false;
		return ((OtaGroupFrame) frame).tryDecrypt(companion.getConfig().getChannels());
	}

	/**
	 * Try to decrypt a TXT_MSG payload in this packet addressed to our node. Only
	 * attempts decryption when {@code destHash} matches the first byte of our own
	 * public key (from {@link SelfInfo}). Iterates contacts whose public-key first
	 * byte matches {@code srcHash} and tries each as the sender. On success the
	 * decrypted data is stored in the underlying {@link OtaUnicastFrame} fields
	 * ({@code decryptedSenderPubKey}, {@code decryptedTimestamp},
	 * {@code decryptedText}, {@code decryptedSecret}).
	 *
	 * @param ourPrivKey64 our 64-byte Ed25519 private key (from
	 *                     CMD_EXPORT_PRIVATE_KEY)
	 * @return true if a sender matched and decryption succeeded
	 */
	public boolean tryDecryptTxtMsg(byte[] ourPrivKey64) {
		if (ourPrivKey64 == null || ourPrivKey64.length < 64)
			return false;
		OtaFrame frame = getOtaFrame();
		if (!(frame instanceof OtaUnicastFrame))
			return false;
		OtaUnicastFrame unicast = (OtaUnicastFrame) frame;
		SelfInfo si = companion.getSelfInfo();
		if (si != null && unicast.destHash != (si.getPubkey()[0] & 0xFF))
			return false;

		for (Contact contact : companion.getConfig().findContacts(new byte[] { (byte) unicast.srcHash }, null)) {
			if (unicast.tryDecryptTxtMsg(contact, ourPrivKey64))
				return true;
		}
		return false;
	}
}
