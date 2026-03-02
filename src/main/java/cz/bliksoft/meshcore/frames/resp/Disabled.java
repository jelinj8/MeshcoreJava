package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

public class Disabled extends ResponseFrame {

	public Disabled(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_DISABLED;
	}

	@Override
	public String toString() {
		return "RESP_DISABLED";
	}
}
