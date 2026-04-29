package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets the advertisement name of the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} response.
 */
public class CmdSetAdvertName extends CommandFrame {

	final String advertName;

	/**
	 * @param name the advertisement name to set (truncated to 32 bytes)
	 */
	public CmdSetAdvertName(String name) {
		advertName = name;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_ADVERT_NAME;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(advertName, 32);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
