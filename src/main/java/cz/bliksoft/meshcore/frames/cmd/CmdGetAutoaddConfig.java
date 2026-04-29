package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that requests the current contacts auto-add configuration from
 * the device. Expects a {@link cz.bliksoft.meshcore.frames.resp.AutoaddConfig}
 * response.
 */
public class CmdGetAutoaddConfig extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_AUTOADD_CONFIG;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { getTypeCode() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_AUTOADD_CONFIG.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
