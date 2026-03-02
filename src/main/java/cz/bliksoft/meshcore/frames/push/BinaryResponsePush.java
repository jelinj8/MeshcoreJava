package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class BinaryResponsePush extends ResponseFrame {

	final byte reserved;

	public byte getReserved() {
		return reserved;
	}

	public byte[] getTag() {
		return tag;
	}

	public byte[] getFrameData() {
		return frameData;
	}

	final byte[] tag;
	final byte[] frameData;

	public BinaryResponsePush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		tag = br.readBytes(4);
		this.frameData = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_BINARY_RESPONSE;
	}

	@Override
	public String toString() {
		return String.format("PUSH_BINARY_RESPONSE reserved=%d tag=%s data=%s", reserved, MeshcoreUtils.hex(tag),
				MeshcoreUtils.hex(frameData));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(tag);
	}
}
