package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.StatsCommandFrameSubtype;

/**
 * Command frame that retrieves device statistics for the specified subtype.
 * Expects a {@link cz.bliksoft.meshcore.frames.resp.Stats} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdGetStats extends CommandFrame {

	final StatsCommandFrameSubtype subtype;

	/**
	 * @param subtype the statistics subtype to request
	 */
	public CmdGetStats(StatsCommandFrameSubtype subtype) {
		this.subtype = subtype;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_STATS;
	}

	@Override
	public byte[] getBytes() {

		return new byte[] { getTypeCode(), subtype.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_STATS.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
