package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class RawDataPush extends ResponseFrame {

	final int lastSnr4;
	final int lastRssi;
	final byte reserved;
	final byte[] rawData;

	public RawDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		lastSnr4 = br.readSignedByte();
		lastRssi = br.readSignedByte();
		reserved = br.readByte();
		rawData = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_RAW_DATA;
	}

	@Override
	public String toString() {
		return String.format("PUSH_RAW_DATA lastSnr=%.2f lastssi=%d reserved=%d data=%s", lastSnr4 / 4.0, lastRssi,
				reserved, MeshcoreUtils.hex(rawData));
	}
}
