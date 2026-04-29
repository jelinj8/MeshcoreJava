package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends a block of data to be signed by the device as part
 * of a signing session. Expects a {@link cz.bliksoft.meshcore.frames.resp.Ok}
 * or {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdSignData extends CommandFrame {

	final byte[] data;

	/**
	 * @param data the data chunk to feed into the active signing session
	 */
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
