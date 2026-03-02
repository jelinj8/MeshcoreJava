package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class PathDiscoveryResponsePush extends ResponseFrame {

	public byte getReserved() {
		return reserved;
	}

	public byte[] getPrefix6() {
		return prefix6;
	}

	public int getOutLen() {
		return outLen;
	}

	public byte[] getOutPath() {
		return outPath;
	}

	public int getInLen() {
		return inLen;
	}

	public byte[] getInPath() {
		return inPath;
	}

	final byte reserved;
	final byte[] prefix6;
	final int outLen;
	final byte[] outPath;
	final int inLen;
	final byte[] inPath;

	public PathDiscoveryResponsePush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		prefix6 = br.readBytes(6);
		outLen = br.readUnsignedByte();
		outPath = br.readBytes(outLen);
		inLen = br.readUnsignedByte();
		inPath = br.readBytes(inLen);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_PATH_DISCOVERY_RESPONSE;
	}

	@Override
	public String toString() {
		return String.format(
				"PUSH_PATH_DISCOVERY_RESPONSE reserved=%d prefix6=%s outLen=%d outPath=%s inLen=%d inPath=%s", reserved,
				MeshcoreUtils.hex(prefix6), outLen, MeshcoreUtils.hex(outPath), inLen, MeshcoreUtils.hex(inPath));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
