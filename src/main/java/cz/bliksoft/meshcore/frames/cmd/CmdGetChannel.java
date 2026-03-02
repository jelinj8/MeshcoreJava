package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdGetChannel extends CommandFrame {

	final int channelId;

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
