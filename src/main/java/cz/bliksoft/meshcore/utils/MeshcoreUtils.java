package cz.bliksoft.meshcore.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * parsing utils
 */
public class MeshcoreUtils {
	private MeshcoreUtils() {
	};

//	public static String readFixedCString(ByteBuffer bb, int size) {
//		byte[] b = new byte[size];
//		bb.get(b);
//		int n = 0;
//		while (n < b.length && b[n] != 0)
//			n++;
//		return new String(b, 0, n, StandardCharsets.US_ASCII);
//	}

//	public static String readFixedCString(byte[] data, int index, int maxSize) {
//		if (data == null)
//			throw new IllegalArgumentException("data == null");
//		if (index < 0 || index >= data.length)
//			throw new IndexOutOfBoundsException("index out of bounds");
//		if (maxSize < 0)
//			throw new IllegalArgumentException("maxSize < 0");
//
//		int end = index;
//		int limit = Math.min(data.length, index + maxSize);
//
//		while (end < limit && data[end] != 0) {
//			end++;
//		}
//
//		return new String(data, index, end - index, StandardCharsets.US_ASCII);
//
//	}

//	public static int readUnsignedByte(byte[] data, int index) {
//		return data[index] & 0xFF;
//	}

//	public static int read24s(byte[] b, int off) {
//		int v = ((b[off] & 0xFF) << 16) | ((b[off + 1] & 0xFF) << 8) | (b[off + 2] & 0xFF);
//		if ((v & 0x800000) != 0)
//			v |= 0xFF000000;
//		return v;
//	}

//	public static int cayenneLen(int type) {
//		// minimal mapping for common types; unknown -> can't safely skip
//		switch (type) {
//		case 0x67:
//			return 2; // temperature
//		case 0x68:
//			return 1; // humidity
//		case 0x02:
//			return 2; // analog input
//		case 0x03:
//			return 2; // analog output
//		case 0x88:
//			return 9; // gps
//		default:
//			return -1;
//		}
//	}

	public static String hexPrefix6(byte[] pubKey32) {
		byte[] p = new byte[6];
		System.arraycopy(pubKey32, 0, p, 0, 6);
		return hex(p);
	}

	public static String hex(byte b) {
		return String.format("%02x", b);
	}

	public static String hex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (byte x : b)
			sb.append(String.format("%02x", x));
		return sb.toString();
	}

	public static String hex(byte[] b, int maxLen) {
		StringBuilder sb = new StringBuilder();
		int idx = 0;
		for (byte x : b) {
			idx++;
			if (idx > maxLen)
				break;
			sb.append(String.format("%02x", x));
		}
		return sb.toString();
	}

	public static byte[] fixedNullTerm(String s, int len) {
		byte[] out = new byte[len];
		if (s == null)
			return out;
		byte[] b = s.getBytes(StandardCharsets.UTF_8);
		int n = Math.min(len - 1, b.length);
		System.arraycopy(b, 0, out, 0, n);
		out[n] = 0;
		return out;
	}

//	public static byte[] intToBytesLE(int value) {
//	    return new byte[] {
//	        (byte) value,
//	        (byte) (value >>> 8),
//	        (byte) (value >>> 16),
//	        (byte) (value >>> 24)
//	    };
//	}
//	
//	public static byte[] uint32ToBytesLE(long value) {
//	    if (value < 0 || value > 0xFFFFFFFFL)
//	        throw new IllegalArgumentException("out of range");
//
//	    return new byte[] {
//	        (byte) value,
//	        (byte) (value >>> 8),
//	        (byte) (value >>> 16),
//	        (byte) (value >>> 24)
//	    };
//	}

//	public static byte[] toCString(String s) {
//		if (s == null)
//			throw new IllegalArgumentException("s == null");
//
//		byte[] str = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
//		byte[] out = new byte[str.length + 1];
//
//		System.arraycopy(str, 0, out, 0, str.length);
//		out[str.length] = 0; // null terminator
//
//		return out;
//	}

//	public static byte[] tail(byte[] frame) {
//		if (frame == null || frame.length <= 1)
//			return new byte[0];
//
//		return java.util.Arrays.copyOfRange(frame, 1, frame.length);
//	}

	public static String formatMeshcoreTs(long epochSeconds) {
		return java.time.Instant.ofEpochSecond(epochSeconds).atZone(java.time.ZoneId.systemDefault())
				.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

//	public static String hex6(byte[] prefix6) {
//		StringBuilder sb = new StringBuilder(12);
//		for (int i = 0; i < prefix6.length; i++)
//			sb.append(String.format("%02x", prefix6[i]));
//		return sb.toString();
//	}

//	public static Map<String, String> parseCustomVars(byte[] p) {
//		String s = new String(p, 1, p.length - 1, StandardCharsets.UTF_8).trim();
//		Map<String, String> m = new LinkedHashMap<>();
//		if (s.isEmpty())
//			return m;
//		for (String part : s.split(",")) {
//			String t = part.trim();
//			if (t.isEmpty())
//				continue;
//			int ix = t.indexOf(':');
//			if (ix < 0)
//				m.put(t, "");
//			else
//				m.put(t.substring(0, ix).trim(), t.substring(ix + 1).trim());
//		}
//		return m;
//	}

	public static byte[] fromHex(String s) {
		if (s == null)
			throw new IllegalArgumentException("hex string is null");

		String cleaned = s.replaceAll("[^0-9A-Fa-f]", "");

		if ((cleaned.length() & 1) != 0)
			throw new IllegalArgumentException("hex string must have even length");

		int len = cleaned.length();
		byte[] out = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			int hi = Character.digit(cleaned.charAt(i), 16);
			int lo = Character.digit(cleaned.charAt(i + 1), 16);

			if (hi < 0 || lo < 0)
				throw new IllegalArgumentException("invalid hex char at " + i);

			out[i / 2] = (byte) ((hi << 4) | lo);
		}

		return out;
	}

	public static byte[] pubKeyPrefix6(byte[] publicKey32) {
		byte[] p = new byte[6];
		System.arraycopy(publicKey32, 0, p, 0, 6);
		return p;
	}

	public static byte[] hashChannelKey(String channelName) {
		byte[] sha;
		try {
			sha = MessageDigest.getInstance("SHA-256").digest("#test".getBytes(StandardCharsets.UTF_8));
			return Arrays.copyOf(sha, 16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isPrefix(byte[] prefix, byte[] data) {
		if (prefix.length > data.length)
			return false;

		for (int i = 0; i < prefix.length; i++) {
			if (prefix[i] != data[i])
				return false;
		}
		return true;
	}

//	public static byte[] bytes(byte[] data, int index, int length) {
//		byte[] p = new byte[length];
//		System.arraycopy(data, index, p, 0, length);
//		return p;
//	}

//	public static int readUInt32LE(byte[] data, int index) {
//		if (index < 0 || index + 4 > data.length)
//			throw new IndexOutOfBoundsException();
//
//		return (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8) | ((data[index + 2] & 0xFF) << 16)
//				| ((data[index + 3] & 0xFF) << 24);
//	}
}
