package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetDeviceTime}
 * reporting the current Unix epoch time as maintained by the device.
 */
public class CurrTime extends ResponseFrame {

	final long timestamp;

	/**
	 * @return current device time as Unix epoch seconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	public CurrTime(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		timestamp = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_CURR_TIME;
	}

	@Override
	public String toString() {
		return String.format("RESP_CURR_TIME %s", MeshcoreUtils.formatMeshcoreTs(timestamp));
	}

}
