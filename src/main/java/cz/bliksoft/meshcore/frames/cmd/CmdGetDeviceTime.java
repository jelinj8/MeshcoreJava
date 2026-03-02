package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdGetDeviceTime extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_DEVICE_TIME;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_CURR_TIME.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
