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
	 * add single byte
	 * 
	 * @param b
	 */
	public void put(byte b) {
		ensure(1);
		buf[size++] = b;
	}

	/**
	 * add bytes
	 * 
	 * @param b
	 */
	public void put(byte[] b) {
		ensure(b.length);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
	}

	/**
	 * add bytes with fixed length, fill missing with zeroes
	 * 
	 * @param b
	 * @param len
	 */
	public void put(byte[] b, int len) {
		ensure(len);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
		if (b.length < len)
			put(new byte[len - b.length]);
	}

	/**
	 * put String as cString
	 * 
	 * @param text
	 */
	public void put(String text) {
		byte[] txt = text.getBytes(StandardCharsets.UTF_8);
		ensure(txt.length + 1);
		put(txt);
		put((byte) 0);
	}

	/**
	 * put String as cString, truncated if longer
	 * 
	 * @param text
	 * @param maxLen
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
	 * put String as cString, fill with zeroes if shorter
	 * 
	 * @param text
	 * @param length
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
	 * put int32 (LE)
	 * 
	 * @param value
	 */
	public void putInt32LE(int value) {
		ensure(4);
		put(new byte[] { (byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24) });
	}

	/**
	 * put uint32
	 * 
	 * @param value
	 */
	public void putUInt32LE(long value) {
		if (value < 0 || value > 0xFFFFFFFFL)
			throw new IllegalArgumentException("out of range");
		ensure(4);
		put(new byte[] { (byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24) });
	}

	/**
	 * get resulting byte[]
	 * 
	 * @return
	 */
	public byte[] toArray() {
		return java.util.Arrays.copyOf(buf, size);
	}
}
