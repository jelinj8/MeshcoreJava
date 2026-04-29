package cz.bliksoft.meshcore.utils;

import java.nio.charset.StandardCharsets;

public class ByteReader {

	private final byte[] data;
	private int index = 0;

	/**
	 * Wrap a byte array for sequential reading.
	 *
	 * @param data bytes to read from
	 */
	public ByteReader(byte[] data) {
		this.data = data;
	}

	public void skip() {
		index++;
	}

	public void skip(int count) {
		index += count;
	}

	/**
	 * Read one byte and return it as an unsigned int (0–255).
	 *
	 * @return the byte value in the range 0–255
	 */
	public int readUnsignedByte() {
		return data[index++] & 0xFF;
	}

	/**
	 * Read one byte (signed, as Java's {@code byte} type).
	 *
	 * @return the signed byte value
	 */
	public byte readByte() {
		return data[index++];
	}

	/**
	 * Read one byte and return it as a signed int (-128–127).
	 *
	 * @return the byte value in the range -128–127
	 */
	public int readSignedByte() {
		return data[index++];
	}

	/**
	 * Read exactly {@code length} bytes.
	 *
	 * @param length number of bytes to read
	 * @return new byte array of the requested length
	 */
	public byte[] readBytes(int length) {
		byte[] p = new byte[length];
		System.arraycopy(data, index, p, 0, length);
		index += length;
		return p;
	}

	/**
	 * Read all remaining bytes to the end of the buffer.
	 *
	 * @return new byte array containing the remaining bytes
	 */
	public byte[] readBytes() {
		byte[] p = new byte[data.length - index];
		System.arraycopy(data, index, p, 0, data.length - index);
		index += data.length - index;
		return p;
	}

	/**
	 * read signed Int16 (little-endian)
	 */
	public short readInt16LE() {
		if (index < 0 || index + 2 > data.length)
			throw new IndexOutOfBoundsException();

		short result = (short) ((data[index] & 0xFF) | (data[index + 1] << 8));
		index += 2;
		return result;
	}

	/**
	 * Read an unsigned 16-bit integer (little-endian) and return it as a
	 * non-negative int (0–65535).
	 */
	public int readUInt16LE() {
		if (index < 0 || index + 2 > data.length)
			throw new IndexOutOfBoundsException();

		int result = (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8);
		index += 2;
		return result;
	}

	/**
	 * Read an unsigned 32-bit integer (little-endian) and return it as a
	 * non-negative long (0–0xFFFFFFFFL).
	 */
	public long readUInt32LE() {
		if (index < 0 || index + 4 > data.length)
			throw new IndexOutOfBoundsException();

		long result = (data[index] & 0xFFL) | ((data[index + 1] & 0xFFL) << 8) | ((data[index + 2] & 0xFFL) << 16)
				| ((data[index + 3] & 0xFFL) << 24);
		index += 4;
		return result;
	}

	public int remaining() {
		return data.length - index;
	}

	/**
	 * Read a signed 32-bit integer (little-endian) and return it as a long
	 * (sign-extended).
	 */
	public long readInt32LE() {
		int result = (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8) | ((data[index + 2] & 0xFF) << 16)
				| (data[index + 3] << 24);
		index += 4;
		return result;
	}

	/**
	 * Read a null-terminated UTF-8 string from a fixed-width field. Always advances
	 * the cursor by {@code maxSize} bytes regardless of string length.
	 *
	 * @param maxSize total field width in bytes (including the null terminator)
	 * @return decoded string (without null terminator)
	 */
	public String readFixedCString(int maxSize) {
		int end = index;
		int limit = Math.min(data.length, index + maxSize);

		while (end < limit && data[end] != 0) {
			end++;
		}
		String result = new String(data, index, end - index, StandardCharsets.UTF_8);
		index += maxSize;
		return result;
	}

}
