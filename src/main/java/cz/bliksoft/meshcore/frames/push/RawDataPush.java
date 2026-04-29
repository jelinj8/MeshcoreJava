package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame delivered when the device receives a raw over-the-air packet that
 * does not map to a higher-level frame type, carrying the SNR, RSSI, and the
 * unprocessed packet bytes.
 */
public class RawDataPush extends ResponseFrame {

	/** Signed int8; SNR of the received packet in dB = lastSnr4 / 4.0. */
	final int lastSnr4;
	/** Signed int8; RSSI of the received packet in dBm. */
	final int lastRssi;
	/** Always 0xFF — reserved by firmware ("possibly path_len in future"). */
	final byte reserved;
	final byte[] rawData;

	public RawDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		lastSnr4 = br.readSignedByte();
		lastRssi = br.readSignedByte();
		reserved = br.readByte();
		rawData = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_RAW_DATA;
	}

	@Override
	public String toString() {
		return String.format("PUSH_RAW_DATA lastSnr=%.2f lastssi=%d reserved=%d data=%s", lastSnr4 / 4.0, lastRssi,
				reserved, MeshcoreUtils.hex(rawData));
	}
}
