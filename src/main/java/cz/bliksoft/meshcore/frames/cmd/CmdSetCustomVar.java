package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetCustomVar extends CommandFrame {

	final String varName;
	final String varValue;

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
