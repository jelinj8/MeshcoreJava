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

	public static String hex(byte[] b, int groupSize, String separator) {
		if (b == null || b.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++) {
			if (i > 0 && i % groupSize == 0)
				sb.append(separator);
			sb.append(String.format("%02x", b[i]));
		}
		return sb.toString();
	}

	public static byte[] prefix6(byte[] pubKey32) {
		byte[] p = new byte[6];
		System.arraycopy(pubKey32, 0, p, 0, 6);
		return p;
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

	public static String formatMeshcoreTs(long epochSeconds) {
		return java.time.Instant.ofEpochSecond(epochSeconds).atZone(java.time.ZoneId.systemDefault())
				.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

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

	public static byte[] hashChannelKey(String channelName) {
		byte[] sha;
		try {
			sha = MessageDigest.getInstance("SHA-256").digest(channelName.getBytes(StandardCharsets.UTF_8));
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

	private static final double EARTH_RADIUS_KM = 6371.0;

	/**
	 * Calculates the great-circle distance between two GPS coordinates using the
	 * Haversine formula.
	 *
	 * @param lat1 latitude of the first point in decimal degrees
	 * @param lon1 longitude of the first point in decimal degrees
	 * @param lat2 latitude of the second point in decimal degrees
	 * @param lon2 longitude of the second point in decimal degrees
	 * @return distance in kilometres
	 */
	public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

}
