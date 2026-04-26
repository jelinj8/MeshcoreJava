package cz.bliksoft.meshcore.frames.cmd;

import java.time.Instant;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.utils.ByteBuilder;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class CmdSendChannelTxtMessage extends CommandFrame {

	/**
	 * Maximum UTF-8 bytes for the message text.
	 * The serial frame header accounts for 7 bytes, but additional lower-level mesh/LoRa framing
	 * further reduces usable payload. Value matches the official companion app limit.
	 */
	public static final int MAX_TEXT_BYTES = 135;

	final MessageTextType txtType;
	final int channelId;
	final long timestamp;
	final String text;

	public CmdSendChannelTxtMessage(MessageTextType txtType, int channelId, Long timestamp, String text) {
		this.txtType = txtType;
		this.channelId = channelId;
		this.timestamp = (timestamp != null ? timestamp : Instant.now().getEpochSecond());
		this.text = text;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_CHANNEL_TXT_MSG;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(txtType.code());
		bb.put((byte) channelId);
		bb.putUInt32LE(timestamp);
		bb.put(text);

		return bb.toArray();
	}

	@Override
	public String toString() {
		return String.format("%s %s #%s->%s", getFrameType(), txtType, channelId, text);
	}

}
