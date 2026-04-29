package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that retrieves the current radio tuning parameters from the
 * device. Expects a {@link cz.bliksoft.meshcore.frames.resp.TuningParams}
 * response.
 */
public class CmdGetTuningParams extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_TUNING_PARAMS;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_GET_TUNING_PARAMS.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_TUNING_PARAMS.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
