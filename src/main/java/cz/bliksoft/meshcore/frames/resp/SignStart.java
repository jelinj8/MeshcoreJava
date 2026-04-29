package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdSignStart} confirming
 * that the device is ready to accept data for signing, and advertising the
 * maximum payload size it can handle.
 */
public class SignStart extends ResponseFrame {

	/**
	 * @return reserved byte (currently unused, value is unspecified)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return maximum number of bytes the device will accept across all
	 *         {@link cz.bliksoft.meshcore.frames.cmd.CmdSignData} frames in this
	 *         signing session
	 */
	public long getMaxDataLength() {
		return maxDataLength;
	}

	final byte reserved;
	final long maxDataLength;

	public SignStart(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		reserved = br.readByte();
		maxDataLength = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SIGN_START;
	}

	@Override
	public String toString() {
		return String.format("RESP_SIGN_START reserved=%d maxDataLen=%d", reserved, maxDataLength);
	}
}
