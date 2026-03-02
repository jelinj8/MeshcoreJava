package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class SendConfirmedPush extends ResponseFrame {

	final byte[] tag;

	public byte[] getTag() {
		return tag;
	}

	public long getTripTime() {
		return tripTime;
	}

	final long tripTime;

	public SendConfirmedPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		this.tag = br.readBytes(4);
		this.tripTime = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_SEND_CONFIRMED;
	}

	@Override
	public String toString() {
		return String.format("PUSH_SEND_CONFIRMED tripTime(ms)=%d tag=%s", tripTime, MeshcoreUtils.hex(tag));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(tag);
	}
}
