package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendChannelData extends CommandFrame {

	private static final byte PATH_LEN_FLOOD = (byte) 0xFF;

	final int channelIdx;
	/** 0xFF = flood/unknown, otherwise encoded path hop count. */
	final byte pathLen;
	/** Encoded path bytes; null when pathLen == 0xFF (flood). */
	final byte[] encodedPath;
	/** 16-bit data type (little-endian). */
	final int dataType;
	final byte[] payload;

	/**
	 * @param channelIdx  index into device channel table
	 * @param pathLen     0xFF for flood, otherwise encoded hop count
	 * @param encodedPath encoded path bytes (null when pathLen == 0xFF)
	 * @param dataType    16-bit data type identifier
	 * @param payload     binary payload
	 */
	public CmdSendChannelData(int channelIdx, byte pathLen, byte[] encodedPath, int dataType, byte[] payload) {
		this.channelIdx = channelIdx;
		this.pathLen = pathLen;
		this.encodedPath = encodedPath;
		this.dataType = dataType;
		this.payload = payload;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_CHANNEL_DATA;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put((byte) channelIdx);
		bb.put(pathLen);
		if (pathLen != PATH_LEN_FLOOD && encodedPath != null) {
			bb.put(encodedPath);
		}
		bb.put((byte) (dataType & 0xFF));
		bb.put((byte) ((dataType >> 8) & 0xFF));
		if (payload != null) {
			bb.put(payload);
		}
		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
