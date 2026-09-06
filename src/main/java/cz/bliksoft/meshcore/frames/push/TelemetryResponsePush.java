package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device in response to a telemetry request, carrying
 * the raw telemetry payload together with a 6-byte node prefix used for
 * response matching.
 */
public class TelemetryResponsePush extends ResponseFrame {

	final byte reserved;

	/**
	 * @return reserved byte from the wire frame (currently unused)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return first 6 bytes of the responding node's public key, used as the
	 *         response key for matching
	 */
	public byte[] getPrefix6() {
		return prefix6;
	}

	/**
	 * @return length in bytes of the telemetry payload
	 */
	public int getTelemetryLen() {
		return telemetryLen;
	}

	/**
	 * @return raw telemetry payload bytes
	 */
	public byte[] getFrameData() {
		return frameData;
	}

	final byte[] prefix6;
	final int telemetryLen;
	final byte[] frameData;

	public TelemetryResponsePush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		this.reserved = br.readByte();
		this.prefix6 = br.readBytes(6);
		// NOTE: the wire frame has no explicit length byte here — firmware writes
		// the raw telemetry payload straight after prefix6 until the frame ends.
		this.frameData = br.readBytes();
		this.telemetryLen = frameData.length;
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_TELEMETRY_RESPONSE;
	}

	@Override
	public String toString() {
		return String.format("PUSH_TELEMETRY_RESPONSE reserved=%d prefix6=%s, len=%d, data=%s", reserved,
				MeshcoreUtils.hex(prefix6), telemetryLen, MeshcoreUtils.hex(frameData));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
