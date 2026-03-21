package cz.bliksoft.meshcore.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic primitives matching the MeshCore firmware (lib/ed25519 +
 * Utils.cpp).
 *
 * <pre>
 * Shared-secret derivation (ed25519_key_exchange):
 *   1. Use first 32 bytes of the 64-byte Ed25519 private key as the X25519 scalar
 *      (already SHA-512 expanded &amp; clamped by ed25519_create_keypair).
 *   2. Convert the other party's Ed25519 public key (Edwards y) to the
 *      Curve25519 Montgomery u-coordinate:
 *        u = (1 + y) * inverse(1 - y)  mod  2^255-19
 *   3. Run the X25519 Montgomery-ladder scalar multiplication.
 *
 * Authenticated decryption (Utils::MACThenDecrypt):
 *   MAC  = HMAC-SHA256(secret[0..31], ciphertext)[0..1]
 *   AES key = secret[0..15]   (AES-128-ECB, no IV)
 * </pre>
 */
public class MeshCoreCrypto {

	/** Minimum accepted Unix epoch timestamp (2020-01-01). */
	public static final long TS_MIN = 1_577_836_800L;
	/** Maximum accepted Unix epoch timestamp (2035-01-01). */
	public static final long TS_MAX = 2_051_222_400L;

	private static final BigInteger P   = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19));
	private static final BigInteger A24 = BigInteger.valueOf(121665); // (A-2)/4, A=486662

	/**
	 * Compute the ECDH shared secret, mirroring
	 * {@code LocalIdentity::calcSharedSecret} / {@code ed25519_key_exchange}.
	 *
	 * @param prv64   our 64-byte Ed25519 private key (from CMD_EXPORT_PRIVATE_KEY)
	 * @param edPub32 their 32-byte Ed25519 public key
	 * @return 32-byte shared secret
	 */
	public static byte[] calcSharedSecret(byte[] prv64, byte[] edPub32) {
		// First 32 bytes of the stored private key are the clamped X25519 scalar.
		byte[] scalar = Arrays.copyOf(prv64, 32);
		byte[] mont = edPubToMontgomery(edPub32);
		return x25519(scalar, mont);
	}

	/**
	 * Verify the 2-byte MAC and decrypt, mirroring {@code Utils::MACThenDecrypt}.
	 *
	 * @param secret32     32-byte shared secret
	 * @param macAndCipher 2-byte HMAC prefix followed by AES-128-ECB ciphertext
	 *                     (must be a multiple of 16 bytes)
	 * @return decrypted plaintext, or {@code null} if the MAC is wrong or input is
	 *         malformed
	 */
	public static byte[] macThenDecrypt(byte[] secret32, byte[] macAndCipher) {
		if (macAndCipher == null || macAndCipher.length < 2 + 16)
			return null;
		try {
			byte[] ciphertext = Arrays.copyOfRange(macAndCipher, 2, macAndCipher.length);
			if (ciphertext.length % 16 != 0)
				return null;

			Mac hmac = Mac.getInstance("HmacSHA256");
			hmac.init(new SecretKeySpec(secret32, "HmacSHA256"));
			byte[] mac = hmac.doFinal(ciphertext);
			if (mac[0] != macAndCipher[0] || mac[1] != macAndCipher[1])
				return null;

			Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
			aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOf(secret32, 16), "AES"));
			return aes.doFinal(ciphertext);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Result of a successful GRP_TXT decryption via {@link #tryDecryptWithKey}.
	 */
	public static final class GrpTxtResult {
		/** Unix epoch seconds (uint32 from plaintext bytes [0..3]). */
		public final long timestamp;
		/** Decoded message text (plaintext bytes [5..end]). */
		public final String text;

		GrpTxtResult(long timestamp, String text) {
			this.timestamp = timestamp;
			this.text = text;
		}
	}

	/**
	 * Attempt to decrypt a GRP_TXT payload with a known 16-byte channel key.
	 *
	 * <p>
	 * Encryption chain:
	 * <ul>
	 * <li>payload[0] = {@code SHA256(key)[0]} (channel hash pre-filter)</li>
	 * <li>payload[1..2] = {@code HMAC-SHA256(key||0x00*16, ciphertext)[0..1]}</li>
	 * <li>payload[3..] = {@code AES-128-ECB(key, plaintext)}</li>
	 * <li>plaintext = {@code timestamp(uint32LE,4B) + txtType(1B) + text}</li>
	 * </ul>
	 *
	 * @param grpTxtPayload raw GRP_TXT payload (channelHash + 2-byte MAC + ciphertext)
	 * @param key           16-byte AES-128 channel key (= {@code SHA256("#name")[0..15]})
	 * @return decoded {@link GrpTxtResult}, or {@code null} if MAC/hash/sanity
	 *         checks fail
	 */
	public static GrpTxtResult tryDecryptWithKey(byte[] grpTxtPayload, byte[] key) {
		if (grpTxtPayload == null || grpTxtPayload.length < 1 + 2 + 16)
			return null;
		try {
			// channelHash pre-filter: SHA256(key)[0]
			Mac hmac = Mac.getInstance("HmacSHA256");
			Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");

			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
			if (md.digest(key)[0] != grpTxtPayload[0])
				return null;

			byte[] ciphertext = Arrays.copyOfRange(grpTxtPayload, 3, grpTxtPayload.length);
			if (ciphertext.length % 16 != 0)
				return null;

			// MAC check: HMAC-SHA256(key||0x00*16, ciphertext)[0..1]
			hmac.init(new SecretKeySpec(Arrays.copyOf(key, 32), "HmacSHA256"));
			byte[] macBytes = hmac.doFinal(ciphertext);
			if (macBytes[0] != grpTxtPayload[1] || macBytes[1] != grpTxtPayload[2])
				return null;

			// AES-128-ECB decrypt
			aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
			byte[] plain = aes.doFinal(ciphertext);

			// timestamp (uint32LE, bytes [0..3])
			long ts = (plain[0] & 0xFFL) | ((plain[1] & 0xFFL) << 8) | ((plain[2] & 0xFFL) << 16)
					| ((plain[3] & 0xFFL) << 24);
			if (ts < TS_MIN || ts > TS_MAX)
				return null;

			// byte [4] is TXT_TYPE — must be 0 (plain text)
			if (plain[4] != 0)
				return null;

			// extract text from byte [5], validate printable chars, check zero-padding
			int textEnd = 5;
			while (textEnd < plain.length && plain[textEnd] != 0) {
				byte b = plain[textEnd];
				if (b > 0 && b < 0x09)
					return null;
				if (b > 0x0D && b < 0x20)
					return null;
				textEnd++;
			}
			if (textEnd == 5)
				return null; // empty text
			if (textEnd < plain.length) {
				for (int j = textEnd + 1; j < plain.length; j++)
					if (plain[j] != 0)
						return null;
			}

			return new GrpTxtResult(ts, new String(plain, 5, textEnd - 5, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return null;
		}
	}

	// ---- private helpers ----

	/**
	 * Convert a 32-byte Ed25519 public key (Edwards compressed y) to the
	 * Curve25519 Montgomery u-coordinate.
	 * u = (1 + y) * inverse(1 - y) mod p
	 */
	private static byte[] edPubToMontgomery(byte[] edPub32) {
		byte[] yBytes = edPub32.clone();
		yBytes[31] &= 0x7F; // clear sign bit — we only need y
		BigInteger y = fromLE(yBytes);
		BigInteger num = BigInteger.ONE.add(y).mod(P);
		BigInteger den = BigInteger.ONE.subtract(y).mod(P); // mod makes negative positive
		return toLE(num.multiply(den.modInverse(P)).mod(P), 32);
	}

	/**
	 * X25519 scalar multiplication — RFC 7748 Montgomery ladder.
	 * Re-clamps the scalar before use (matches ed25519_key_exchange behaviour).
	 */
	private static byte[] x25519(byte[] kBytes, byte[] uBytes) {
		byte[] e = kBytes.clone();
		e[0] &= 248;
		e[31] &= 63;
		e[31] |= 64;

		byte[] ub = uBytes.clone();
		ub[31] &= 0x7F; // clear unused high bit

		BigInteger x1 = fromLE(ub);
		BigInteger x2 = BigInteger.ONE;
		BigInteger z2 = BigInteger.ZERO;
		BigInteger x3 = x1;
		BigInteger z3 = BigInteger.ONE;
		int swap = 0;

		for (int pos = 254; pos >= 0; pos--) {
			int b = (e[pos / 8] >> (pos & 7)) & 1;
			swap ^= b;
			if (swap != 0) {
				BigInteger t;
				t = x2; x2 = x3; x3 = t;
				t = z2; z2 = z3; z3 = t;
			}
			swap = b;

			BigInteger A  = x2.add(z2).mod(P);
			BigInteger AA = A.multiply(A).mod(P);
			BigInteger B  = x2.subtract(z2).mod(P);
			BigInteger BB = B.multiply(B).mod(P);
			BigInteger E  = AA.subtract(BB).mod(P);
			BigInteger C  = x3.add(z3).mod(P);
			BigInteger D  = x3.subtract(z3).mod(P);
			BigInteger DA = D.multiply(A).mod(P);
			BigInteger CB = C.multiply(B).mod(P);
			BigInteger s  = DA.add(CB).mod(P);
			x3 = s.multiply(s).mod(P);
			BigInteger d  = DA.subtract(CB).mod(P);
			z3 = x1.multiply(d.multiply(d).mod(P)).mod(P);
			x2 = AA.multiply(BB).mod(P);
			z2 = E.multiply(AA.add(A24.multiply(E).mod(P)).mod(P)).mod(P);
		}

		if (swap != 0) {
			BigInteger t;
			t = x2; x2 = x3; x3 = t;
			t = z2; z2 = z3; z3 = t;
		}

		return toLE(x2.multiply(z2.modInverse(P)).mod(P), 32);
	}

	/** Decode a little-endian byte array as an unsigned BigInteger. */
	private static BigInteger fromLE(byte[] bytes) {
		byte[] rev = new byte[bytes.length + 1]; // rev[0] = 0 sign byte
		for (int i = 0; i < bytes.length; i++)
			rev[bytes.length - i] = bytes[i];
		return new BigInteger(rev);
	}

	/** Encode a BigInteger as a little-endian byte array of exactly {@code len} bytes. */
	private static byte[] toLE(BigInteger n, int len) {
		byte[] be = n.toByteArray(); // big-endian, may have a leading zero sign byte
		byte[] out = new byte[len];
		for (int dst = 0, src = be.length - 1; dst < len && src >= 0; dst++, src--)
			out[dst] = be[src];
		return out;
	}
}
