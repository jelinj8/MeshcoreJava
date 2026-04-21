package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdGetDefaultFloodScope extends CommandFrame {

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_DEFAULT_FLOOD_SCOPE.code() };

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_DEFAULT_FLOOD_SCOPE;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
