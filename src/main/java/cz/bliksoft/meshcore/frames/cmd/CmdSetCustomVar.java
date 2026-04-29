package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets a custom variable on the device by name and value.
 * Expects a {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdSetCustomVar extends CommandFrame {

	final String varName;
	final String varValue;

	/**
	 * @param name  the name of the custom variable to set
	 * @param value the value to assign to the custom variable
	 */
	public CmdSetCustomVar(String name, String value) {
		this.varName = name;
		this.varValue = value;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_CUSTOM_VAR;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(varName + ":" + varValue);

		return bb.toArray();
	}

}
