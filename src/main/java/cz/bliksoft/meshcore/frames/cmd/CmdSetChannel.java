package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class CmdSetChannel extends CommandFrame {

	final byte id;
	final String name;
	final byte[] key;

	/**
	 * @param name 32b
	 * @param key  16b supported in v13, null for public channel or #channel (that
	 *             would be 16B of sha256 from name, computed automatically)
	 */
	public CmdSetChannel(int id, String name, byte[] key) {
		this.id = (byte) id;
		this.name = name;
		if (key == null) {
			if (name.startsWith("#") && key == null) {
				this.key = MeshcoreUtils.hashChannelKey(name);
			} else {
				this.key = new byte[16];
			}
		} else {
			this.key = key;
		}
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_CHANNEL;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(id);
		bb.putFixed(name, 32);
		bb.put(key); // should be 16B

		return bb.toArray();
	}

}
