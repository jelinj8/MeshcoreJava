package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AutoAddConfigFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;

public class CmdSetAutoaddConfig extends CommandFrame {

	final byte autoaddCfg;

	/**
	 * set contacts autoadd config
	 * 
	 * @param config use {@link AutoAddConfigFlags}
	 */
	public CmdSetAutoaddConfig(byte config) {
		this.autoaddCfg = config;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_AUTOADD_CONFIG;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { getTypeCode(), autoaddCfg };
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
