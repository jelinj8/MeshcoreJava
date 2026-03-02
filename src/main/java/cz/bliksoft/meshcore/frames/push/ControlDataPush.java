package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class ControlDataPush extends ResponseFrame {

	public int getLastSnr4() {
		return lastSnr4;
	}

	public int getLastRssi() {
		return lastRssi;
	}

	public int getPathLen() {
		return pathLen;
	}

	public byte[] getPayload() {
		return payload;
	}

	final int lastSnr4;
	final int lastRssi;
	final int pathLen;
	final byte[] payload;

	public ControlDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		this.lastSnr4 = br.readSignedByte();
		this.lastRssi = br.readSignedByte();
		this.pathLen = br.readUInt16LE();
		this.payload = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_CONTROL_DATA;
	}

	@Override
	public String toString() {
		return String.format("PUSH_CONTROL_DATA lastSnr=%.2f lastRssi=%d pathLen=%d data=%s", lastSnr4 / 4.0, lastRssi,
				pathLen, MeshcoreUtils.hex(payload));
	}

}
