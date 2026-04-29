package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets the radio transmit power of the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdSetRadioTXPower extends CommandFrame {

	final byte power;

	/**
	 * @param powerDbm the desired transmit power in dBm
	 */
	public CmdSetRadioTXPower(byte powerDbm) {
		this.power = powerDbm;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_RADIO_TX_POWER;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(power);

		return bb.toArray();
	}

}
