package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Generic success acknowledgement sent by the device when a command completes
 * without returning data.
 */
public class Ok extends ResponseFrame {

	public Ok(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_OK;
	}

	@Override
	public String toString() {
		return "RESP_OK";
	}
}
