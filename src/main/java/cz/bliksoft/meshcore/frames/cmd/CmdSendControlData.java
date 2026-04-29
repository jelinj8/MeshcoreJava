package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends a raw control data payload to the device for
 * firmware-defined processing. Does not expect a specific response frame.
 */
public class CmdSendControlData extends CommandFrame {

	final byte[] data;

	/**
	 * @param data control payload; the first byte must have bit 7 (0x80) set as
	 *             required by the firmware
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
