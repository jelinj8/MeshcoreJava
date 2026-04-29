package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame received when a remote node replies to a binary request, carrying
 * the request tag for correlation and the raw binary response payload.
 */
public class BinaryResponsePush extends ResponseFrame {

	final byte reserved;

	/**
	 * @return reserved byte from the wire frame (currently unused)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return 4-byte request tag matching the originating CMD_SEND_BINARY_REQ /
	 *         CMD_SEND_ANON_REQ
	 */
	public byte[] getTag() {
		return tag;
	}

	/**
	 * @return raw binary payload returned by the remote node
	 */
	public byte[] getFrameData() {
		return frameData;
	}

	/**
	 * Request tag — matches
	 * {@link cz.bliksoft.meshcore.frames.resp.Sent#getAckIdOrTag()} of the
	 * originating CMD_SEND_BINARY_REQ / CMD_SEND_ANON_REQ.
	 */
	final byte[] tag;
	/** Raw binary payload returned by the remote node. */
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
