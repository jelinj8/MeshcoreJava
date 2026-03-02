package cz.bliksoft.meshcore.frames;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

public class UnparsedFrame extends ResponseFrame {

	public UnparsedFrame(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public byte getTypeCode() {
		return data[0];
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.fromByte(data[0]);
	}

}
