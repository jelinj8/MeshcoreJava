package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class StatusResponsePush extends ResponseFrame {

	public byte getReserved() {
		return reserved;
	}

	public byte[] getPrefix6() {
		return prefix6;
	}

	public byte[] getRawData() {
		return rawData;
	}

	final byte reserved;
	final byte[] prefix6;
	final byte[] rawData;

	public StatusResponsePush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		prefix6 = br.readBytes(6);
		rawData = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_STATUS_RESPONSE;
	}

	@Override
	public String toString() {
		return String.format("PUSH_STATUS_RESPONSE reserved=%d prefix6=%s dfata=%s", reserved,
				MeshcoreUtils.hex(prefix6), MeshcoreUtils.hex(rawData));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
