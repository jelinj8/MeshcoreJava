package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetTuningParams extends CommandFrame {

	final double rxDelayBase;
	final double airtimeFactor;

	public CmdSetTuningParams(double rxDelayBase, double airtimeFactor) {
		this.rxDelayBase = rxDelayBase;
		this.airtimeFactor = airtimeFactor;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_TUNING_PARAMS;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.putUInt32LE(Math.round(rxDelayBase * 1000));
		bb.putUInt32LE(Math.round(airtimeFactor * 1000));

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
