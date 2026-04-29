package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device when a path-discovery probe completes, carrying
 * the outbound and inbound hop sequences to the discovered node together with
 * its 6-byte key prefix.
 */
public class PathDiscoveryResponsePush extends ResponseFrame {

	/**
	 * @return reserved byte from the wire frame (currently unused)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return first 6 bytes of the discovered node's public key, used as the
	 *         response key for matching
	 */
	public byte[] getPrefix6() {
		return prefix6;
	}

	/**
	 * @return number of bytes per hash entry in the outbound path (1, 2, or 3)
	 */
	public int getOutHashLength() {
		return outHashLength;
	}

	/**
	 * @return number of hops in the outbound path (this node to discovered node)
	 */
	public int getOutPathLength() {
		return outPathLength;
	}

	/**
	 * @return concatenated node-hash entries for the outbound path
	 */
	public byte[] getOutPath() {
		return outPath;
	}

	/**
	 * @return number of bytes per hash entry in the inbound (return) path (1, 2, or
	 *         3)
	 */
	public int getInHashLength() {
		return inHashLength;
	}

	/**
	 * @return number of hops in the inbound (return) path (discovered node to this
	 *         node)
	 */
	public int getInPathLength() {
		return inPathLength;
	}

	/**
	 * @return concatenated node-hash entries for the inbound (return) path
	 */
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
