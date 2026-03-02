package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class Signature extends ResponseFrame {

	final byte[] signature;

	public byte[] getSignature() {
		return signature;
	}

	public Signature(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		signature = br.readBytes(64);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SIGNATURE;
	}

}
