package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetRadioTXPower extends CommandFrame {

	final byte power;

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
