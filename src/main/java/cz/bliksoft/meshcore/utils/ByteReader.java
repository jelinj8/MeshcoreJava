package cz.bliksoft.meshcore.utils;

import java.nio.charset.StandardCharsets;

public class ByteReader {

	private final byte[] data;
	private int index = 0;

	/**
	 * initialize reader with data
	 * 
	 * @param data
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
	 * read byte as unsigned int
	 * 
	 * @return
	 */
	public int readUnsignedByte() {
		return data[index++] & 0xFF;
	}

	/**
	 * read byte
	 * 
	 * @return
	 */
	public byte readByte() {
		return data[index++];
	}

	/**
	 * read byte as signed int
	 * 
	 * @return
	 */
	public int readSignedByte() {
		return data[index++];
	}

	/**
	 * read byte count
	 * 
	 * @param length
	 * @return
	 */
	public byte[] readBytes(int length) {
		byte[] p = new byte[length];
		System.arraycopy(data, index, p, 0, length);
		index += length;
		return p;
	}

	/**
	 * read all remaining bytes
	 * 
	 * @return
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
	 * read unsigned Int32 (little-endian)
	 * 
	 * @return
	 */
	public int readUInt16LE() {
		if (index < 0 || index + 2 > data.length)
			throw new IndexOutOfBoundsException();

		int result = (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8);
		index += 2;
		return result;
	}

	/**
	 * read unsigned Int32 (little-endian)
	 * 
	 * @return
	 */
	public long readUInt32LE() {
		if (index < 0 || index + 4 > data.length)
			throw new IndexOutOfBoundsException();

		int result = (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8) | ((data[index + 2] & 0xFF) << 16)
				| ((data[index + 3] & 0xFF) << 24);
		index += 4;
		return result;
	}

	/**
	 * read signed Int32 (little-endian)
	 * 
	 * @return
	 */
	public long readInt32LE() {
		int result = (data[index] & 0xFF) | ((data[index + 1] & 0xFF) << 8) | ((data[index + 2] & 0xFF) << 16)
				| (data[index + 3] << 24);
		index += 4;
		return result;
	}

	/**
	 * read string up to null char, maxSize bytes or data end
	 * 
	 * @param maxSize
	 * @return
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
