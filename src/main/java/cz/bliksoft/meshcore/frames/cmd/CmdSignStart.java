package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that initiates a data-signing session on the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.SignStart} response.
 */
public class CmdSignStart extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SIGN_START;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_SIGN_START.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_SIGN_START.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
