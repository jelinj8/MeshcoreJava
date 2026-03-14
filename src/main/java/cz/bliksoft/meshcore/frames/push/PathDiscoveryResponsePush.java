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

	public int getOutHashLength() {
		return outHashLength;
	}

	public int getOutPathLength() {
		return outPathLength;
	}

	public byte[] getOutPath() {
		return outPath;
	}

	public int getInHashLength() {
		return inHashLength;
	}

	public int getInPathLength() {
		return inPathLength;
	}

	public byte[] getInPath() {
		return inPath;
	}

	final byte reserved;
	/**
	 * First 6 bytes of the discovered node's public key; used as response key for
	 * matching.
	 */
	final byte[] prefix6;
	/** Bytes per path-hash entry in the outbound path: 1, 2, or 3. */
	final int outHashLength;
	/** Number of hops in the outbound path (this node → discovered node). */
	final int outPathLength;
	/**
	 * Concatenated node-hash entries for the outbound path (this node → discovered
	 * node).
	 */
	final byte[] outPath;
	/** Bytes per path-hash entry in the inbound path: 1, 2, or 3. */
	final int inHashLength;
	/**
	 * Number of hops in the inbound (return) path (discovered node → this node).
	 */
	final int inPathLength;
	/**
	 * Concatenated node-hash entries for the inbound (return) path (discovered node
	 * → this node).
	 */
	final byte[] inPath;

	public PathDiscoveryResponsePush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		prefix6 = br.readBytes(6);
		int outRaw = br.readUnsignedByte();
		outHashLength = (outRaw >> 6) + 1;
		outPathLength = outRaw & 0x3F;
		outPath = br.readBytes(outPathLength * outHashLength);
		int inRaw = br.readUnsignedByte();
		inHashLength = (inRaw >> 6) + 1;
		inPathLength = inRaw & 0x3F;
		inPath = br.readBytes(inPathLength * inHashLength);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_PATH_DISCOVERY_RESPONSE;
	}

	@Override
	public String toString() {
		return String.format(
				"PUSH_PATH_DISCOVERY_RESPONSE reserved=%d prefix6=%s outLen=%d outPath=%s inLen=%d inPath=%s", reserved,
				MeshcoreUtils.hex(prefix6), outPathLength, MeshcoreUtils.hex(outPath, outHashLength, "-"), inPathLength,
				MeshcoreUtils.hex(inPath, inHashLength, "-"));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
