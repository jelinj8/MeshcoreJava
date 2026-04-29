package cz.bliksoft.meshcore.otaframe;

import java.nio.charset.StandardCharsets;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshCoreCrypto;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Unicast encrypted payload: REQ, RESPONSE, TXT_MSG, PATH.
 *
 * <pre>
 *   dest_hash(1)  src_hash(1)  MAC(2)  ciphertext(N*16)
 * </pre>
 *
 * <p>
 * After a successful call to {@link #tryDecryptTxtMsg} the fields
 * {@link #decryptedTimestamp}, {@link #decryptedText}, and
 * {@link #decryptedSecret} are populated.
 */
public class OtaUnicastFrame extends OtaFrame {

	/** First byte of the destination node's public key. */
	public final int destHash;
	/** First byte of the source node's public key. */
	public final int srcHash;
	/** 2-byte MAC followed by AES-128-ECB ciphertext. */
	public final byte[] macAndCipher;

	/** sender contact. */
	public Contact decryptedSender = null;
	/** Unix epoch seconds of the decrypted message, or -1 if not yet decrypted. */
	public long decryptedTimestamp = -1;
	/** Plaintext message, or {@code null} if not yet decrypted. */
	public String decryptedText = null;
	/** 32-byte ECDH shared secret used for decryption, or {@code null}. */
	public byte[] decryptedSecret = null;

	OtaUnicastFrame(MeshcoreCompanion source, OtaRouteType route, OtaPayloadType payloadType, int ver, int tc0, int tc1,
			int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
		ByteReader br = new ByteReader(payloadBytes);
		if (br.remaining() >= 2) {
			destHash = br.readUnsignedByte();
			srcHash = br.readUnsignedByte();
			macAndCipher = br.readBytes();
		} else {
			destHash = -1;
			srcHash = -1;
			macAndCipher = new byte[0];
		}
	}

	/**
	 * Try to decrypt this TXT_MSG payload using the given sender public key and our
	 * private key. On success the decrypted data is stored in
	 * {@link #decryptedTimestamp}, {@link #decryptedText},
	 * {@link #decryptedSender}, and {@link #decryptedSecret}.
	 *
	 * <p>
	 * Only meaningful when {@link #payloadType} is {@link OtaPayloadType#TXT_MSG}.
	 *
	 * <p>
	 * Plaintext layout:
	 * {@code [timestamp(4)] [flags(1)] [text...] [null?] [zero-padding]}.
	 * {@code flags = attempt & 3} — upper 6 bits encode TXT_TYPE (0 = plain).
	 *
	 * @param contact      contact whose 32-byte Ed25519 public key is used for decryption
	 * @param ourPrivKey64 our 64-byte Ed25519 private key (from
	 *                     CMD_EXPORT_PRIVATE_KEY)
	 * @return true if MAC matched and plaintext was valid
	 */
	public boolean tryDecryptTxtMsg(Contact contact, byte[] ourPrivKey64) {
		byte[] senderPubKey = contact.getPubkey();

		if (payloadType != OtaPayloadType.TXT_MSG)
			return false;

		if (senderPubKey == null || senderPubKey.length < 32)
			return false;
		if (ourPrivKey64 == null || ourPrivKey64.length < 64)
			return false;
		if (macAndCipher.length < 18)
			return false;

		byte[] secret = MeshCoreCrypto.calcSharedSecret(ourPrivKey64, senderPubKey);
		byte[] plain = MeshCoreCrypto.macThenDecrypt(secret, macAndCipher);
		if (plain == null || plain.length < 6)
			return false;

		long ts = (plain[0] & 0xFFL) | ((plain[1] & 0xFFL) << 8) | ((plain[2] & 0xFFL) << 16)
				| ((plain[3] & 0xFFL) << 24);
		if (ts < MeshCoreCrypto.TS_MIN || ts > MeshCoreCrypto.TS_MAX)
			return false;

		// upper 6 bits of flags encode TXT_TYPE; 0 = plain
		if ((plain[4] & 0xFC) != 0)
			return false;

		int textEnd = 5;
		while (textEnd < plain.length && plain[textEnd] != 0) {
			byte b = plain[textEnd];
			if ((b > 0 && b < 0x09) || (b > 0x0D && b < 0x20))
				return false;
			textEnd++;
		}
		if (textEnd <= 5)
			return false;

		decryptedSender = contact;
		decryptedSecret = secret;
		decryptedTimestamp = ts;
		decryptedText = new String(plain, 5, textEnd - 5, StandardCharsets.UTF_8);
		return true;
	}

	@Override
	public String toString() {
		if (destHash < 0)
			return routingPrefix() + " <incomplete>";
		int encLen = macAndCipher.length - 2;
		if (payloadType == OtaPayloadType.TXT_MSG)
			return String.format("%s src=%02x dest=%02x enc=%dB data=%s", routingPrefix(), srcHash, destHash, encLen,
					decryptedText == null ? MeshcoreUtils.hex(payloadBytes)
							: String.format("[%s:%s]%s", MeshcoreUtils.hexPrefix6(decryptedSender.getPubkey()),
									decryptedSender.getName(), decryptedText));
		else
			return String.format("%s src=%02x dest=%02x enc=%dB data=%s", routingPrefix(), srcHash, destHash, encLen,
					MeshcoreUtils.hex(payloadBytes));
	}
}
