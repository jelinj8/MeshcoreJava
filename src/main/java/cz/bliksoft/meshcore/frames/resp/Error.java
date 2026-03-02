package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ErrorFrameResultCode;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

public class Error extends ResponseFrame {

	final ErrorFrameResultCode code;

	public ErrorFrameResultCode getCode() {
		return code;
	}

	public Error(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		code = ErrorFrameResultCode.fromByte(data[1]);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_ERR;
	}

	@Override
	public String toString() {
		return String.format("ERROR: %s", code);
	}
}
