package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AutoAddConfigFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;

/**
 * Command frame that sets the contacts auto-add configuration on the device.
 * Expects an OK response.
 */
public class CmdSetAutoaddConfig extends CommandFrame {

	final byte autoaddCfg;
	/** -1 = omit (firmware keeps existing value), 0–64 = set. */
	final int maxHops;

	/**
	 * Set contacts auto-add config without changing maxHops.
	 *
	 * @param config bitmask, see {@link AutoAddConfigFlags}
	 */
	public CmdSetAutoaddConfig(byte config) {
		this.autoaddCfg = config;
		this.maxHops = 0;
	}

	/**
	 * Set contacts auto-add config and maxHops.
	 *
	 * @param config  bitmask, see {@link AutoAddConfigFlags}
	 * @param maxHops maximum hop count for auto-add (0–64)
	 */
	public CmdSetAutoaddConfig(byte config, int maxHops) {
		this.autoaddCfg = config;
		this.maxHops = maxHops;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_AUTOADD_CONFIG;
	}

	@Override
	public byte[] getBytes() {
		if (maxHops >= 0)
			return new byte[] { getTypeCode(), autoaddCfg, (byte) Math.min(maxHops, 64) };
		return new byte[] { getTypeCode(), autoaddCfg };
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
