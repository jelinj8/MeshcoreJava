package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendControlData extends CommandFrame {

	final byte[] data;

	/**
	 * 
	 * @param data data[0]|0x80 != 0
	 */
	public CmdSendControlData(byte[] data) {
		this.data = data;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_CONTROL_DATA;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(data);
		return bb.toArray();
	}

}
