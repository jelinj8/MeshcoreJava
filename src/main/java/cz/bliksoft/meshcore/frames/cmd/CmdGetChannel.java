package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that requests configuration and details for a specific channel.
 * Expects a {@link cz.bliksoft.meshcore.frames.resp.ChannelInfo} response, or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} on failure.
 */
public class CmdGetChannel extends CommandFrame {

	final int channelId;

	/**
	 * @param channelId the index of the channel to retrieve
	 */
	public CmdGetChannel(int channelId) {
		this.channelId = channelId;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_CHANNEL;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put((byte) channelId);

		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_CHANNEL_INFO.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
