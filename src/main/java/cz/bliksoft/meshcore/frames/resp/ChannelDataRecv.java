package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Push notification delivered when the device receives a raw data payload on a
 * channel, carrying the channel index, routing information, data type, and raw
 * payload bytes.
 */
public class ChannelDataRecv extends MessageFrameGroup {

	/**
	 * @return raw SNR value from the firmware (signed int8); divide by 4.0 to get
	 *         dB
	 */
	public int getSnr4() {
		return snr4;
	}

	/**
	 * @return zero-based index into the device's channel table
	 */
	public int getChannelIdx() {
		return channelIdx;
	}

	/**
	 * 0xFF = message arrived via direct (non-flood) route; any other value = flood
	 * path hop count.
	 */
	public int getPathLen() {
		return pathLen;
	}

	/**
	 * @return {@code true} if the data arrived via a flood route, {@code false} for
	 *         a direct route
	 */
	public boolean isFlood() {
		return pathLen != 0xFF;
	}

	/** 16-bit data type identifier (little-endian from firmware). */
	public int getDataType() {
		return dataType;
	}

	/**
	 * @return raw data payload bytes for this data frame
	 */
	public byte[] getData() {
		return data;
	}

	/** Signed int8 from firmware; SNR in dB = snr4 / 4.0. */
	final int snr4;
	final int channelIdx;
	final int pathLen;
	final int dataType;
	final byte[] data;

	public ChannelDataRecv(MeshcoreCompanion source, byte[] raw) {
		super(source, raw);
		ByteReader br = new ByteReader(raw);
		br.skip(); // code byte
		snr4 = br.readByte(); // signed int8
		br.readByte(); // reserved1
		br.readByte(); // reserved2
		channelIdx = br.readUnsignedByte();
		pathLen = br.readUnsignedByte();
		int lo = br.readUnsignedByte();
		int hi = br.readUnsignedByte();
		dataType = lo | (hi << 8);
		int dataLen = br.readUnsignedByte();
		data = br.readBytes(dataLen);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_CHANNEL_DATA_RECV;
	}

	@Override
	public String toString() {
		return String.format("%s channel=%d snr=%.2f pathLen=%d flood=%b dataType=0x%04X dataLen=%d", getFrameType(),
				channelIdx, snr4 / 4.0, pathLen, isFlood(), dataType, data != null ? data.length : 0);
	}
}
