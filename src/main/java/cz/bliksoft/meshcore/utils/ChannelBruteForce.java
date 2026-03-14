package cz.bliksoft.meshcore.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Brute-force discovery of MeshCore group channel names from captured GRP_TXT
 * packets.
 *
 * <p>
 * Encryption chain (from firmware Utils.cpp / Mesh.cpp):
 * 
 * <pre>
 *   key         = SHA256("#channelName")[0..15]   (16-byte AES-128 key)
 *   channelHash = SHA256(key)[0]                  (1-byte packet prefix)
 *   plaintext   = timestamp(uint32LE, 4B) + text + zero-padding to 16B boundary
 *   ciphertext  = AES-128-ECB(key, plaintext)     (no IV — fully deterministic)
 *   mac         = HMAC-SHA256(key||0x00*16, ciphertext)[0..1]  (2 bytes)
 *
 *   GRP_TXT payload = [channelHash(1B)] [mac(2B)] [ciphertext]
 *
 *   HMAC key is 32 bytes: the 16-byte AES key zero-padded to PUB_KEY_SIZE=32
 *   (GroupChannel.secret is memset to 0 before the 16-byte key is written in).
 * </pre>
 *
 * <p>
 * Three-stage filter:
 * <ol>
 * <li>channelHash pre-filter — rejects 255/256 candidates (1 SHA-256)</li>
 * <li>MAC check — HMAC-SHA256 of ciphertext, rejects 65535/65536 (2
 * SHA-256)</li>
 * <li>AES decrypt + timestamp/text sanity — essentially unreachable for wrong
 * keys</li>
 * </ol>
 *
 * <p>
 * Search space with 39-char alphabet (a-z, 0-9, -, @, :):
 * <ul>
 * <li>len 3–5: trivial (&lt; 1 s)</li>
 * <li>len 6: ~3.5B candidates / 1.4M per-thread SHA-256 → a few seconds
 * multi-threaded</li>
 * <li>len 7: ~137B → minutes</li>
 * <li>len 8+: hours → consider wordlist mode instead</li>
 * </ul>
 */
public class ChannelBruteForce {

	/** Allowed characters in channel names (case-sensitive, lower-case a-z). */
	private static final char[] CHARSET;
	static {
		StringBuilder sb = new StringBuilder();
		for (char c = 'a'; c <= 'z'; c++)
			sb.append(c);
		for (char c = '0'; c <= '9'; c++)
			sb.append(c);
		sb.append('-');
		sb.append('@');
		sb.append(':');
		sb.append('_');
		CHARSET = sb.toString().toCharArray();
	}

	/**
	 * Valid Unix epoch range used to accept decrypted timestamps (2020-01-01 ..
	 * 2035-01-01).
	 */
	private static final long TS_MIN = 1_577_836_800L;
	private static final long TS_MAX = 2_051_222_400L;

	// ThreadLocal pools so we don't allocate Cipher/MessageDigest/Mac per
	// candidate.
	private static final ThreadLocal<MessageDigest> TL_SHA256 = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	});
	private static final ThreadLocal<Cipher> TL_AES = ThreadLocal.withInitial(() -> {
		try {
			return Cipher.getInstance("AES/ECB/NoPadding");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	});
	private static final ThreadLocal<Mac> TL_HMAC = ThreadLocal.withInitial(() -> {
		try {
			return Mac.getInstance("HmacSHA256");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	});

	public interface MatchCallback {
		/**
		 * Called (possibly from multiple threads) when a matching channel name is
		 * found.
		 */
		void onMatch(String channelName, byte[] key, long timestamp, String text);

		/**
		 * Called once on the calling thread after all combinations have been tried or
		 * the stop signal has been set.
		 *
		 * @param matched number of confirmed matches found during this run
		 * @param stopped true if terminated early via the stop signal, false if
		 *                exhausted
		 */
		default void onComplete(long matched, boolean stopped) {
		}
	}

	/**
	 * Brute-force channel name discovery from one captured GRP_TXT payload.
	 *
	 * @param grpTxtPayload raw payload bytes starting with the 1-byte channel hash
	 * @param minLen        minimum name length to try (3 recommended)
	 * @param maxLen        maximum name length to try (6 for minutes)
	 * @param callback      called for each match found; {@code onComplete} called
	 *                      when done
	 * @param stopSignal    set to {@code true} to abort early; may be {@code null}
	 */
	public static void bruteForce(byte[] grpTxtPayload, int minLen, int maxLen, MatchCallback callback,
			AtomicBoolean stopSignal) throws InterruptedException {
		if (grpTxtPayload.length < 1 + 2 + 16) // channelHash + MAC + at least one AES block
			throw new IllegalArgumentException("Payload too short to be a valid GRP_TXT");

		byte channelHashTarget = grpTxtPayload[0];
		byte mac0 = grpTxtPayload[1];
		byte mac1 = grpTxtPayload[2];
		byte[] ciphertext = Arrays.copyOfRange(grpTxtPayload, 3, grpTxtPayload.length);
		if (ciphertext.length % 16 != 0)
			throw new IllegalArgumentException("Ciphertext length not a multiple of AES block size (16)");

		AtomicLong tried = new AtomicLong();
		AtomicLong matched = new AtomicLong();

		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		for (int len = minLen; len <= maxLen; len++) {
			final int nameLen = len;
			// Parallelize by first character
			for (char firstChar : CHARSET) {
				final char fc = firstChar;
				pool.submit(() -> {
					if (stopSignal != null && stopSignal.get())
						return;
					char[] name = new char[nameLen];
					name[0] = fc;
					searchRecursive(name, 1, channelHashTarget, mac0, mac1, ciphertext, callback, tried, matched,
							stopSignal);
				});
			}
		}

		pool.shutdown();
		pool.awaitTermination(72, TimeUnit.HOURS);
		callback.onComplete(matched.get(), stopSignal != null && stopSignal.get());
	}

	/** Convenience overload without a stop signal. */
	public static void bruteForce(byte[] grpTxtPayload, int minLen, int maxLen, MatchCallback callback)
			throws InterruptedException {
		bruteForce(grpTxtPayload, minLen, maxLen, callback, null);
	}

	// --- internal ---

	private static void searchRecursive(char[] name, int pos, byte channelHashTarget, byte mac0, byte mac1,
			byte[] ciphertext, MatchCallback callback, AtomicLong tried, AtomicLong matched, AtomicBoolean stopSignal) {
		if (stopSignal != null && stopSignal.get())
			return;
		if (pos == name.length) {
			tried.incrementAndGet();
			tryCandidate(new String(name), channelHashTarget, mac0, mac1, ciphertext, callback, matched);
			return;
		}
		for (char c : CHARSET) {
			name[pos] = c;
			searchRecursive(name, pos + 1, channelHashTarget, mac0, mac1, ciphertext, callback, tried, matched,
					stopSignal);
		}
	}

	private static void tryCandidate(String name, byte channelHashTarget, byte mac0, byte mac1, byte[] ciphertext,
			MatchCallback callback, AtomicLong matched) {
		try {
			MessageDigest md = TL_SHA256.get();

			// Step 1: key = SHA256("#name")[0..15]
			md.reset();
			byte[] key = Arrays.copyOf(md.digest(("#" + name).getBytes(StandardCharsets.UTF_8)), 16);

			// Step 2: channelHash = SHA256(key)[0] — fast pre-filter (rejects 255/256)
			md.reset();
			if (md.digest(key)[0] != channelHashTarget)
				return;

			// Step 3: MAC check — HMAC-SHA256(key||0x00*16, ciphertext)[0..1]
			// GroupChannel.secret is uint8_t[PUB_KEY_SIZE=32], memset to 0 then key written
			// into [0..15]
			Mac hmac = TL_HMAC.get();
			hmac.init(new SecretKeySpec(Arrays.copyOf(key, 32), "HmacSHA256"));
			byte[] macBytes = hmac.doFinal(ciphertext);
			if (macBytes[0] != mac0 || macBytes[1] != mac1)
				return;

			// Step 4: AES-128-ECB decrypt
			Cipher aes = TL_AES.get();
			aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
			byte[] plain = aes.doFinal(ciphertext);

			// Step 5: validate timestamp (uint32LE in bytes [0..3])
			long ts = (plain[0] & 0xFFL) | ((plain[1] & 0xFFL) << 8) | ((plain[2] & 0xFFL) << 16)
					| ((plain[3] & 0xFFL) << 24);
			if (ts < TS_MIN || ts > TS_MAX)
				return;

			// Step 6: byte 4 is TXT_TYPE (0 = plain); must be 0 for valid GRP_TXT
			if (plain[4] != 0)
				return;

			// Step 7: extract text starting at byte 5 (format: "senderName: message[\0]")
			// Note: when text exactly fills the AES block there is no null terminator in
			// the
			// ciphertext (firmware passes data_len without the trailing '\0' to
			// encryptThenMAC).
			int textEnd = 5;
			while (textEnd < plain.length && plain[textEnd] != 0) {
				byte b = plain[textEnd];
				// reject C0 control chars except tab/LF/CR; high bytes (0x80+) are valid UTF-8
				if (b > 0 && b < 0x09)
					return;
				if (b > 0x0D && b < 0x20)
					return;
				textEnd++;
			}
			if (textEnd == 5)
				return; // empty text
			// if a null terminator is present, all subsequent bytes must be zero
			// (zero-padding)
			if (textEnd < plain.length) {
				for (int j = textEnd + 1; j < plain.length; j++)
					if (plain[j] != 0)
						return;
			}

			String text = new String(plain, 5, textEnd - 5, StandardCharsets.UTF_8);

			matched.incrementAndGet();
			callback.onMatch("#" + name, key, ts, text);

		} catch (Exception e) {
			// ignore errors
		}
	}

	/** Convenience: decode a hex GRP_TXT payload string and run brute-force. */
	public static void bruteForceHex(String hexPayload, int minLen, int maxLen, MatchCallback callback)
			throws InterruptedException {
		bruteForce(MeshcoreUtils.fromHex(hexPayload), minLen, maxLen, callback, null);
	}

	/**
	 * Convenience: decode a hex GRP_TXT payload string and run brute-force with
	 * stop signal.
	 */
	public static void bruteForceHex(String hexPayload, int minLen, int maxLen, MatchCallback callback,
			AtomicBoolean stopSignal) throws InterruptedException {
		bruteForce(MeshcoreUtils.fromHex(hexPayload), minLen, maxLen, callback, stopSignal);
	}

	/**
	 * Attempts to decrypt a GRP_TXT payload with a known key.
	 *
	 * @param grpTxtPayload raw GRP_TXT payload (channelHash + MAC + ciphertext)
	 * @param channelName   channel name passed verbatim to callback (e.g.
	 *                      "#public")
	 * @param key           16-byte AES-128 key (= SHA256("#name")[0..15])
	 * @param callback      called if decryption and validation succeed
	 * @return true if decryption succeeded
	 */
	public static boolean tryDecryptWithKey(byte[] grpTxtPayload, String channelName, byte[] key,
			MatchCallback callback) {
		if (grpTxtPayload.length < 1 + 2 + 16)
			return false;
		try {
			// channelHash pre-filter
			MessageDigest md = TL_SHA256.get();
			md.reset();
			if (md.digest(key)[0] != grpTxtPayload[0])
				return false;

			byte[] ciphertext = Arrays.copyOfRange(grpTxtPayload, 3, grpTxtPayload.length);
			if (ciphertext.length % 16 != 0)
				return false;

			// MAC check
			Mac hmac = TL_HMAC.get();
			hmac.init(new SecretKeySpec(Arrays.copyOf(key, 32), "HmacSHA256"));
			byte[] macBytes = hmac.doFinal(ciphertext);
			if (macBytes[0] != grpTxtPayload[1] || macBytes[1] != grpTxtPayload[2])
				return false;

			// AES-128-ECB decrypt
			Cipher aes = TL_AES.get();
			aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
			byte[] plain = aes.doFinal(ciphertext);

			long ts = (plain[0] & 0xFFL) | ((plain[1] & 0xFFL) << 8) | ((plain[2] & 0xFFL) << 16)
					| ((plain[3] & 0xFFL) << 24);
			if (ts < TS_MIN || ts > TS_MAX)
				return false;

			if (plain[4] != 0)
				return false; // TXT_TYPE must be 0 (plain)

			int textEnd = 5;
			while (textEnd < plain.length && plain[textEnd] != 0) {
				byte b = plain[textEnd];
				if (b > 0 && b < 0x09)
					return false;
				if (b > 0x0D && b < 0x20)
					return false;
				textEnd++;
			}
			if (textEnd == 5)
				return false;
			if (textEnd < plain.length) {
				for (int j = textEnd + 1; j < plain.length; j++)
					if (plain[j] != 0)
						return false;
			}

			String text = new String(plain, 5, textEnd - 5, StandardCharsets.UTF_8);
			callback.onMatch(channelName, key, ts, text);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
