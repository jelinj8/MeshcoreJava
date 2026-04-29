package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetTuningParams}
 * exposing the device's low-level radio scheduling parameters: receive-delay
 * base and airtime scaling factor.
 */
public class TuningParams extends ResponseFrame {

	/**
	 * @return base receive-delay in seconds (firmware value divided by 1000)
	 */
	public double getRxDelayBase() {
		return rxDelayBase;
	}

	/**
	 * @return airtime scaling factor (firmware value divided by 1000)
	 */
	public double getAirtimeFactor() {
		return airtimeFactor;
	}

	final double rxDelayBase;
	final double airtimeFactor;

	public TuningParams(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		rxDelayBase = br.readUInt32LE() / 1000.0;
		airtimeFactor = br.readUInt32LE() / 1000.0;
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_TUNING_PARAMS;
	}

}
