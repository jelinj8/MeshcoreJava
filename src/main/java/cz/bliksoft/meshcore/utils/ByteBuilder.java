package cz.bliksoft.meshcore.utils;

import java.nio.charset.StandardCharsets;

public class ByteBuilder {
	private byte[] buf = new byte[256];
	private int size;

	private void ensure(int n) {
		if (size + n > buf.length) {
			buf = java.util.Arrays.copyOf(buf, Math.max(buf.length * 2, size + n));
		}
	}

	/**
	 * Append a single byte.
	 *
	 * @param b byte to append
	 */
	public void put(byte b) {
		ensure(1);
		buf[size++] = b;
	}

	/**
	 * Append a byte array.
	 *
	 * @param b bytes to append
	 */
	public void put(byte[] b) {
		ensure(b.length);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
	}

	/**
	 * Append {@code b} and zero-pad to exactly {@code len} bytes.
	 *
	 * @param b   bytes to write
	 * @param len total number of bytes written (b.length bytes of data + padding)
	 */
	public void put(byte[] b, int len) {
		ensure(len);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
		if (b.length < len)
			put(new byte[len - b.length]);
	}

	/**
	 * Append a UTF-8 string followed by a null terminator.
	 *
	 * @param text string to write
	 */
	public void put(String text) {
		byte[] txt = text.getBytes(StandardCharsets.UTF_8);
		ensure(txt.length + 1);
		put(txt);
		put((byte) 0);
	}

	/**
	 * Append a UTF-8 C-string truncated to at most {@code maxLen} bytes (including
	 * the null terminator).
	 *
	 * @param text   string to write
	 * @param maxLen maximum total bytes including the null terminator
	 */
	public void put(String text, int maxLen) {
		byte[] txt = text.getBytes(StandardCharsets.UTF_8);
		if (txt.length >= maxLen) {
			txt[maxLen - 1] = (byte) 0;
			put(txt, maxLen);
		} else {
			ensure(txt.length + 1);
			put(txt);
			put((byte) 0);
		}
	}

	/**
	 * Append a UTF-8 string as a fixed-width null-terminated field, zero-padded to
	 * exactly {@code length} bytes.
	 *
	 * @param text   string to write
	 * @param length exact number of bytes to write (truncated or padded with zeroes
	 *               as needed)
	 */
	public void putFixed(String text, int length) {
		ensure(length);
		byte[] txt = MeshcoreUtils.fixedNullTerm(text, length);
		put(txt);
		if (txt.length < length) {
			put(new byte[length - txt.length]);
		}
	}

	/**
	 * Append a signed 32-bit integer in little-endian byte order.
	 *
	 * @param value value to write
	 */
	public void putInt32LE(int value) {
		ensure(4);
		put(new byte[] { (byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24) });
	}

	/**
	 * Append an unsigned 32-bit integer in little-endian byte order.
	 *
	 * @param value value to write (must be in range 0–0xFFFFFFFFL)
	 */
	public void putUInt32LE(long value) {
		if (value < 0 || value > 0xFFFFFFFFL)
			throw new IllegalArgumentException("out of range");
		ensure(4);
		put(new byte[] { (byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24) });
	}

	/**
	 * Returns the accumulated bytes as a new array of exactly the right size.
	 */
	public byte[] toArray() {
		return java.util.Arrays.copyOf(buf, size);
	}
}
