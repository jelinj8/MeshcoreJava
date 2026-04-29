package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetBattAndStorage}
 * containing battery voltage and flash-storage statistics reported by the
 * device.
 */
public class BattAndStorage extends ResponseFrame {

	/**
	 * @return battery voltage in millivolts
	 */
	public int getMillivolts() {
		return millivolts;
	}

	/**
	 * @return number of kilobytes currently used on the device's storage
	 */
	public long getUsedStorage() {
		return usedStorage;
	}

	/**
	 * @return total storage capacity of the device in kilobytes
	 */
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
		return String.format("RESP_BATT_AND_STORAGE %dmV, %d/%d kB free", millivolts, usedStorage, totalStorage);
	}
}