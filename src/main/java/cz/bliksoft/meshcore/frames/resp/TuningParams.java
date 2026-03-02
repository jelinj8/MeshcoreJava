package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class TuningParams extends ResponseFrame {

	public double getRxDelayBase() {
		return rxDelayBase;
	}

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
