package cz.bliksoft.meshcore.frames.push;

import java.util.Arrays;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants;
import cz.bliksoft.meshcore.frames.FrameConstants.OtaRouteType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

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
//		return String.format("%s snr:%.2f rssi:%d data:%s", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi,
//				MeshcoreUtils.hex(rawData));
		return String.format("%s snr:%.2f rssi:%d %s", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi,
				decodeRawPacket(rawData));
	}

	@SuppressWarnings("unused")
	public static String decodeRawPacket(byte[] raw) {
		int i = 0;
		int header = raw[i++] & 0xFF;
		FrameConstants.OtaRouteType route = FrameConstants.OtaRouteType.fromByte((byte) (header & 0x03));
		FrameConstants.OtaPayloadType payload = FrameConstants.OtaPayloadType.fromByte((byte) ((header >> 2) & 0x0F));
		int ver = (header >> 6) & 0x03;

		boolean hasTransport = (route == OtaRouteType.TRANSPORT_FLOOD || route == OtaRouteType.TRANSPORT_DIRECT);

		// transportCodes - region keys
		int tc0 = 0, tc1 = 0;
		if (hasTransport) {
			tc0 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
			tc1 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
		}

		int pathLenEncoded = raw[i++] & 0xFF;
		int hashSize = (pathLenEncoded >> 6) + 1;
		int hashCount = pathLenEncoded & 0x3F;
		int pathBytes = hashCount * hashSize;
		byte[] path = Arrays.copyOfRange(raw, i, i + pathBytes);
		i += pathBytes;

		byte[] payloadData = Arrays.copyOfRange(raw, i, raw.length);

		return String.format("route=%s type=%s ver=%d path=%s payload=%s", route, payload, ver,
				path.length > 0 ? MeshcoreUtils.hex(path, hashSize, "-") : "direct", MeshcoreUtils.hex(payloadData));
	}
}
