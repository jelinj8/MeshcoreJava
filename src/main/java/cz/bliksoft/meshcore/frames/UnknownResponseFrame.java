package cz.bliksoft.meshcore.frames;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class UnknownResponseFrame extends ResponseFrame {

	public UnknownResponseFrame(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public String toString() {
		return String.format("RESPONSE(%s)", MeshcoreUtils.hex(data));
	}

	@Override
	public byte getTypeCode() {
		return data[0];
	}

	@Override
	public ResponseFrameType getFrameType() {
		return null;
	}

}
