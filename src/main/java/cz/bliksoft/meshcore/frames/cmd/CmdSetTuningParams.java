package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets the radio tuning parameters (RX delay base and
 * airtime factor) on the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} response.
 */
public class CmdSetTuningParams extends CommandFrame {

	final double rxDelayBase;
	final double airtimeFactor;

	/**
	 * @param rxDelayBase   base receive delay in seconds (encoded as milliseconds ×
	 *                      1000)
	 * @param airtimeFactor airtime scaling factor (encoded as value × 1000)
	 */
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
