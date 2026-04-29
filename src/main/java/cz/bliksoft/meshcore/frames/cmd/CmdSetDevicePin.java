package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetDevicePin extends CommandFrame {

	final long pin;

	public static final long AUTO_PIN = 1000000l;

	/**
	 * @param pin desired PIN value, or {@link #AUTO_PIN} (1 000 000) to let the
	 *            device generate one
	 */
	public CmdSetDevicePin(long pin) {
		this.pin = pin;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_DEVICE_PIN;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.putUInt32LE(pin);
		return bb.toArray();
	}

}
