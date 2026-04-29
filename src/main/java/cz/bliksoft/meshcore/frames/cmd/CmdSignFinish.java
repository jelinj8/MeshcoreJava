package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that finalises the active signing session and retrieves the
 * computed signature. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Signature} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdSignFinish extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SIGN_FINISH;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_SIGN_FINISH.code() };
	}

	public final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_SIGNATURE.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
