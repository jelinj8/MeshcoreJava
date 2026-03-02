package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSignData extends CommandFrame {

	final byte[] data;

	public CmdSignData(byte[] data) {
		this.data = data;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SIGN_DATA;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(data);
		return bb.toArray();
	}

}
