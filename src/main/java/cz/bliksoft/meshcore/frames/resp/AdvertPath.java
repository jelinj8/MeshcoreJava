package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetAdvertPath}
 * containing the stored outbound path to a contact, including its timestamp and
 * hop-by-hop hash data.
 */
public class AdvertPath extends ResponseFrame {

	/**
	 * Returns the timestamp when this advert was last received by the node.
	 *
	 * @return Unix epoch seconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the number of hops in the stored path to this contact.
	 *
	 * @return hop count
	 */
	public int getPathLen() {
		return pathLen;
	}

	/**
	 * Returns concatenated node-hash entries for the path.
	 *
	 * @return raw path bytes; total length is {@code pathLen × hashSize}
	 */
	public byte[] getPath() {
		return path;
	}

	/** Unix epoch seconds when this advert was last received by the node. */
	final long timestamp;

	/** Number of hops in the stored path to this contact. */
	final int pathLen;

	final int hashSize;

	/**
	 * Concatenated node-hash entries for the path to this contact. Total bytes =
	 * pathLen × hash_size_per_hop (determined by path_hash_mode).
	 */
	final byte[] path;

	/**
	 * @param source the companion that produced this frame; @param data raw frame
	 *               bytes
	 */
	public AdvertPath(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		timestamp = br.readUInt32LE();
		pathLen = br.readUnsignedByte();
		path = br.readBytes();
		hashSize = path.length / pathLen;
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_ADVERT_PATH;
	}

	@Override
	public String toString() {
		return String.format("RESP_ADVERT_PATH %s len=%d %s", MeshcoreUtils.formatMeshcoreTs(timestamp), pathLen,
				MeshcoreUtils.hex(path, hashSize, "-"));
	}
}
