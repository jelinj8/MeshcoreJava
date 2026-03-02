package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;

public class CmdReboot extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_REBOOT;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_REBOOT.code() };
	}

	@Override
	public byte[] expectedResponses() {
		return null;
	}
}
