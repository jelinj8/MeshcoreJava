package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class AdvertPath extends ResponseFrame {

	public long getTimestamp() {
		return timestamp;
	}

	public int getPathLen() {
		return pathLen;
	}

	public byte[] getPath() {
		return path;
	}

	/** Unix epoch seconds when this advert was last received by the node. */
	final long timestamp;
	/** Number of hops in the stored path to this contact. */
	final int pathLen;
	/**
	 * Concatenated node-hash entries for the path to this contact.
	 * Total bytes = pathLen × hash_size_per_hop (determined by path_hash_mode).
	 */
	final byte[] path;

	public AdvertPath(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		timestamp = br.readUInt32LE();
		pathLen = br.readUnsignedByte();
		path = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_ADVERT_PATH;
	}

	@Override
	public String toString() {
		return String.format("RESP_ADVERT_PATH %s len=%d %s", MeshcoreUtils.formatMeshcoreTs(timestamp), pathLen,
				MeshcoreUtils.hex(path));
	}
}
