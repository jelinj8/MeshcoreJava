package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

public class CmdExportPrivateKey extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_EXPORT_PRIVATE_KEY;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_EXPORT_PRIVATE_KEY.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_PRIVATE_KEY.code(),
			ResponseFrameType.RESP_DISABLED.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
