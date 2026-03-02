package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class LogRXDataPush extends ResponseFrame {

	final int snr4;

	public int getSnr4() {
		return snr4;
	}

	public int getRssi() {
		return rssi;
	}

	public byte[] getRawData() {
		return rawData;
	}

	final int rssi;
	final byte[] rawData;

	public LogRXDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		snr4 = br.readSignedByte();
		rssi = br.readSignedByte();
		rawData = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_LOG_RX_DATA;
	}

	@Override
	public String toString() {
		return String.format("%s snr:%.2f rssi:%d", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi);
	}
}
