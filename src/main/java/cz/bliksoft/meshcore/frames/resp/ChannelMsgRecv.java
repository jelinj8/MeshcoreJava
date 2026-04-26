package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class ChannelMsgRecv extends MessageFrameGroup {

	public ResponseFrameType getType() {
		return type;
	}

	public int getSnr4() {
		return snr4;
	}

	public byte getReserved1() {
		return reserved1;
	}

	public byte getReserved2() {
		return reserved2;
	}

	public int getChannelIdx() {
		return channelIdx;
	}

	public MessageTextType getTextType() {
		return textType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isFlood() {
		return pathLen != 0xFF;
	}

	/**
	 * 255=non-flood
	 * 
	 * @return
	 */
	public int getPathLen() {
		return pathLen;
	}

	public String getText() {
		return text;
	}

	final ResponseFrameType type;
	/**
	 * Signed int8 from firmware; SNR in dB = snr4 / 4.0. Only valid for V3 frames.
	 */
	final int snr4;
	final byte reserved1;
	final byte reserved2;

	/** Index into the device's channel table (0-based). */
	final int channelIdx;
	final MessageTextType textType;
	/** Unix epoch seconds as reported by the sender. */
	final long timestamp;
	/**
	 * 0xFF (255) = message arrived via a direct (non-flood) route. Any other value
	 * = hop count of the flood path the message traversed.
	 */
	final int pathLen;
	final String text;

	public ChannelMsgRecv(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		type = ResponseFrameType.fromByte(br.readByte());
		if (type == ResponseFrameType.RESP_CHANNEL_MSG_RECV_V3) {
			snr4 = br.readByte(); // signed int8_t from firmware
			reserved1 = br.readByte();
			reserved2 = br.readByte();
		} else {
			snr4 = 0;
			reserved1 = 0;
			reserved2 = 0;
		}

		channelIdx = br.readUnsignedByte();
		pathLen = br.readUnsignedByte();
		textType = MessageTextType.fromByte(br.readByte());
		timestamp = br.readUInt32LE();
		text = br.readFixedCString(Settings.MAX_FRAME_SIZE);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return type;
	}

	@Override
	public String toString() {
		ChannelInfo ch = companion.getConfig().getChannel(channelIdx);
		String channelName = (ch != null ? ch.getName() : "?");

		return String.format("%s %s channel=%d:%s timestamp=%s snr=%.2f pathLen=%d flood=%b text=%s", getFrameType(),
				textType, channelIdx, channelName, MeshcoreUtils.formatMeshcoreTs(timestamp), snr4 / 4.0, pathLen,
				isFlood(), text);
	}

}
