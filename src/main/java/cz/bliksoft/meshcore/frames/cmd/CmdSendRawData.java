package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends raw binary data along an explicit mesh path. No
 * response is expected.
 */
public class CmdSendRawData extends CommandFrame {

	final byte[] data;
	final int pathLen;
	final byte[] path;

	/**
	 * @param pathLen the number of hops in the path
	 * @param path    the byte array containing the routing path
	 * @param data    the raw payload to send
	 */
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
