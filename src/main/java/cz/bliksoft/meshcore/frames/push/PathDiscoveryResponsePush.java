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
	/** First 6 bytes of the discovered node's public key; used as response key for matching. */
	final byte[] prefix6;
	/** Number of hops in the outbound path (this node → discovered node). */
	final int outLen;
	/**
	 * Concatenated node-hash entries for the outbound path (this node → discovered node).
	 * Total bytes = outLen × hash_size_per_hop (determined by path_hash_mode).
	 */
	final byte[] outPath;
	/** Number of hops in the inbound (return) path (discovered node → this node). */
	final int inLen;
	/**
	 * Concatenated node-hash entries for the inbound (return) path (discovered node → this node).
	 * Total bytes = inLen × hash_size_per_hop (determined by path_hash_mode).
	 */
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
