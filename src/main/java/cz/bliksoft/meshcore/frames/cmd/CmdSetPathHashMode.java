package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;

/**
 * CMD_SET_PATH_HASH_MODE (61, v10+)
 *
 * <p>Configures the path-hash mode used when flooding packets.
 *
 * <p>Frame layout:
 * <pre>
 *   [1] CMD_SET_PATH_HASH_MODE (0x3D / 61)
 *   [1] reserved – must be 0x00
 *   [1] mode     – path hash mode, 0–2:
 *         0  single hash per hop  (path_hash_mode + 1 = 1 hash)
 *         1  two hashes per hop   (path_hash_mode + 1 = 2 hashes)
 *         2  three hashes per hop (path_hash_mode + 1 = 3 hashes)
 * </pre>
 *
 * <p>Firmware rejects values >= 3 with ERR_CODE_ILLEGAL_ARG.
 * On success responds with RESP_OK; the new value is visible in
 * {@link cz.bliksoft.meshcore.frames.resp.DeviceInfo#getPathHashMode()} on the
 * next CMD_DEVICE_QUERY.
 */
public class CmdSetPathHashMode extends CommandFrame {

	/** Valid path hash mode values (0–2). */
	public static final byte MODE_MIN = 0;
	public static final byte MODE_MAX = 2;

	final byte mode;

	/**
	 * @param mode path hash mode, must be 0, 1 or 2
	 * @throws IllegalArgumentException if mode is outside [0, 2]
	 */
	public CmdSetPathHashMode(byte mode) {
		if (mode < MODE_MIN || mode > MODE_MAX)
			throw new IllegalArgumentException("path hash mode must be 0–2, got: " + mode);
		this.mode = mode;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_PATH_HASH_MODE;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { getTypeCode(), 0x00, mode };
	}

}
