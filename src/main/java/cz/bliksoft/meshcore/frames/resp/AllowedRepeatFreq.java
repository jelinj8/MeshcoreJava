package cz.bliksoft.meshcore.frames.resp;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class AllowedRepeatFreq extends ResponseFrame {

	public List<List<Long>> getRanges() {
		return ranges;
	}

	final List<List<Long>> ranges;

	public AllowedRepeatFreq(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		ranges = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			List<Long> range = new ArrayList<>(2);
			range.add(br.readUInt32LE());
			range.add(br.readUInt32LE());
			ranges.add(range);
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_ALLOWED_REPEAT_FREQ;
	}

}
