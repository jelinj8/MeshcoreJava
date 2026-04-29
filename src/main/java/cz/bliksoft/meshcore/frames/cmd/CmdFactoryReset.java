package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that performs a factory reset of the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdFactoryReset extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_FACTORY_RESET;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put("reset");
		return bb.toArray();
	}

}
