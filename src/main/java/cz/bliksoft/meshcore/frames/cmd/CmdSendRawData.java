package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendRawData extends CommandFrame {

	final byte[] data;
	final int pathLen;
	final byte[] path;

	public CmdSendRawData(int pathLen, byte[] path, byte[] data) {
		this.pathLen = pathLen;
		this.path = path;
		this.data = data;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_RAW_DATA;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());

		bb.put((byte) pathLen);
		bb.put(path);
		bb.put(data);
		return bb.toArray();
	}
}
