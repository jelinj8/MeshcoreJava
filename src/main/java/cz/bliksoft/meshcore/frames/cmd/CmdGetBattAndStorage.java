package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

public class CmdGetBattAndStorage extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_BATT_AND_STORAGE;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_GET_BATT_AND_STORAGE.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_BATT_AND_STORAGE.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
