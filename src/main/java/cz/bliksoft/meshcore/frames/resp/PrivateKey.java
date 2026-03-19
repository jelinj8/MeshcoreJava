package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.utils.ByteReader;

public class PrivateKey extends ResponseFrame {

	final byte[] privateKey;

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public PrivateKey(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		privateKey = br.readBytes(64);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_PRIVATE_KEY;
	}

	@Override
	public String toString() {
		return String.format("RESP_PRIVATE_KEY %s", "***************");
	}
}
