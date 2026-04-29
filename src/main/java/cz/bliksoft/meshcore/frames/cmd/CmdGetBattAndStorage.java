package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that requests the current battery level and storage usage from
 * the device. Expects a {@link cz.bliksoft.meshcore.frames.resp.BattAndStorage}
 * response.
 */
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
