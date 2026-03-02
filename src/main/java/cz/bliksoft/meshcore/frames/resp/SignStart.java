package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class SignStart extends ResponseFrame {

	public byte getReserved() {
		return reserved;
	}

	public long getMaxDataLength() {
		return maxDataLength;
	}

	final byte reserved;
	final long maxDataLength;

	public SignStart(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		reserved = br.readByte();
		maxDataLength = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SIGN_START;
	}

	@Override
	public String toString() {
		return String.format("RESP_SIGN_START reserved=%d maxDataLen=%d", reserved, maxDataLength);
	}
}
