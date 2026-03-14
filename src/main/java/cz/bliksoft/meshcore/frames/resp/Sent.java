package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class Sent extends ResponseFrame {

	public boolean isFlood() {
		return flood;
	}

	public byte[] getAckIdOrTag() {
		return tag;
	}

	public long getExpectedTimeout() {
		return expectedTimeout;
	}

	/** true = packet was sent as a flood, false = sent direct. */
	final boolean flood;
	/**
	 * 4-byte ACK hash (for plain messages) or request tag (for login/binary reqs).
	 * Matches {@link SendConfirmedPush#getTag()} when the recipient ACKs the message.
	 * All zeros when no ACK is expected (e.g. CLI data).
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
