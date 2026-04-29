package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push notification delivered when the device receives a text message on a
 * channel; covers both V1/V2 ({@code RESP_CHANNEL_MSG_RECV}) and V3
 * ({@code RESP_CHANNEL_MSG_RECV_V3}) variants.
 */
public class ChannelMsgRecv extends MessageFrameGroup {

	/**
	 * @return the exact response-frame type ({@code RESP_CHANNEL_MSG_RECV} or
	 *         {@code RESP_CHANNEL_MSG_RECV_V3})
	 */
	public ResponseFrameType getType() {
		return type;
	}

	/**
	 * @return raw SNR value from the firmware (signed int8); divide by 4.0 to get
	 *         dB; only valid for V3 frames
	 */
	public int getSnr4() {
		return snr4;
	}

	/**
	 * @return reserved byte 1 from the V3 frame header (always 0 for V1/V2)
	 */
	public byte getReserved1() {
		return reserved1;
	}

	/**
	 * @return reserved byte 2 from the V3 frame header (always 0 for V1/V2)
	 */
	public byte getReserved2() {
		return reserved2;
	}

	/**
	 * @return zero-based index into the device's channel table
	 */
	public int getChannelIdx() {
		return channelIdx;
	}

	/**
	 * @return encoding/type of the message text payload
	 */
	public MessageTextType getTextType() {
		return textType;
	}

	/**
	 * @return Unix epoch seconds as reported by the sender
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return {@code true} if the message arrived via a flood route, {@code false}
	 *         for a direct route
	 */
	public boolean isFlood() {
		return pathLen != 0xFF;
	}

	/**
	 * Raw path-length byte from the firmware. 255 (0xFF) means the message arrived
	 * via a direct (non-flood) route; any other value is the flood hop count. Use
	 * {@link #isFlood()} for a boolean check.
	 */
	public int getPathLen() {
		return pathLen;
	}

	/**
	 * @return decoded text content of the channel message
	 */
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
