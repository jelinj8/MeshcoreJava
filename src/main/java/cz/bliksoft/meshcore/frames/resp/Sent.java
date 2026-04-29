package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Confirmation response indicating that the device has successfully transmitted
 * a message or request, exposing the routing mode, ACK tag, and estimated
 * delivery timeout.
 */
public class Sent extends ResponseFrame {

	/**
	 * @return {@code true} if the packet was sent as a flood, {@code false} if sent
	 *         direct
	 */
	public boolean isFlood() {
		return flood;
	}

	/**
	 * @return 4-byte ACK hash (for plain messages) or request tag (for login/binary
	 *         requests); all zeros when no ACK is expected (e.g. CLI data)
	 */
	public byte[] getAckIdOrTag() {
		return tag;
	}

	/**
	 * @return estimated timeout in milliseconds after which no ACK or response
	 *         should be expected
	 */
	public long getExpectedTimeout() {
		return expectedTimeout;
	}

	/** true = packet was sent as a flood, false = sent direct. */
	final boolean flood;
	/**
	 * 4-byte ACK hash (for plain messages) or request tag (for login/binary reqs).
	 * Matches {@link SendConfirmedPush#getTag()} when the recipient ACKs the
	 * message. All zeros when no ACK is expected (e.g. CLI data).
	 */
	final byte[] tag;
	/** Estimated timeout in ms after which no ACK/response should be expected. */
	final long expectedTimeout;

	public Sent(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		flood = (br.readUnsignedByte() > 0);

		tag = br.readBytes(4);
		expectedTimeout = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SENT;
	}

	@Override
	public String toString() {
		return String.format("RESP_SENT: flood=%b tag=%s expTimeout(ms)=%d", flood, MeshcoreUtils.hex(tag),
				expectedTimeout);
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(tag);
	}
}
