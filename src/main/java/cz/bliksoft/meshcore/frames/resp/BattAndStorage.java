package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class BattAndStorage extends ResponseFrame {

	public int getMillivolts() {
		return millivolts;
	}

	public long getUsedStorage() {
		return usedStorage;
	}

	public long getTotalStorage() {
		return totalStorage;
	}

	final int millivolts;
	final long usedStorage;
	final long totalStorage;

	public BattAndStorage(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		millivolts = br.readUInt16LE();
		usedStorage = br.readUInt32LE();
		totalStorage = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_BATT_AND_STORAGE;
	}

	@Override
	public String toString() {
		return String.format("RESP_BATT_AND_STORAGE %dmV, %d/%d B", millivolts, usedStorage, totalStorage);
	}
}