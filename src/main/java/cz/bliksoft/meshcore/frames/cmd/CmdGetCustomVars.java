package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that requests all custom variables stored on the device.
 * Expects a {@link cz.bliksoft.meshcore.frames.resp.CustomVars} response.
 */
public class CmdGetCustomVars extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_CUSTOM_VARS;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_GET_CUSTOM_VARS.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_CUSTOM_VARS.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
