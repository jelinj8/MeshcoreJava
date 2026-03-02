package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class AllowedRepeatFreq extends ResponseFrame {

	public long getLowerFreq() {
		return lowerFreq;
	}

	public long getUpperFreq() {
		return upperFreq;
	}

	final long lowerFreq;
	final long upperFreq;

	public AllowedRepeatFreq(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		lowerFreq = br.readUInt32LE();
		upperFreq = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_ALLOWED_REPEAT_FREQ;
	}

}
