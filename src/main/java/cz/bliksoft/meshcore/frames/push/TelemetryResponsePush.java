package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class TelemetryResponsePush extends ResponseFrame {

	final byte reserved;

	public byte getReserved() {
		return reserved;
	}

	public byte[] getPrefix6() {
		return prefix6;
	}

	public int getTelemetryLen() {
		return telemetryLen;
	}

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
		this.telemetryLen = br.readUnsignedByte();
		this.frameData = br.readBytes(telemetryLen);
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
