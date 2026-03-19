package cz.bliksoft.meshcore.otaframe;

import java.util.Arrays;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;

/**
 * TRACE payload: trace_tag(4 LE) auth_code(4 LE) flags(1) snr_data(...).
 */
public class OtaTraceFrame extends OtaFrame {

	public final long tag;
	public final long authCode;
	public final int flags;
	/** SNR readings collected at each hop. */
	public final byte[] snrData;

	OtaTraceFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, OtaPayloadType.TRACE, ver, tc0, tc1, hashSize, path, payloadBytes);
		if (payloadBytes.length >= 9) {
			tag      = (payloadBytes[0] & 0xFFL) | ((payloadBytes[1] & 0xFFL) << 8)
					 | ((payloadBytes[2] & 0xFFL) << 16) | ((payloadBytes[3] & 0xFFL) << 24);
			authCode = (payloadBytes[4] & 0xFFL) | ((payloadBytes[5] & 0xFFL) << 8)
					 | ((payloadBytes[6] & 0xFFL) << 16) | ((payloadBytes[7] & 0xFFL) << 24);
			flags    = payloadBytes[8] & 0xFF;
			snrData  = Arrays.copyOfRange(payloadBytes, 9, payloadBytes.length);
		} else {
			tag = authCode = -1; flags = -1; snrData = new byte[0];
		}
	}

	@Override
	public String toString() {
		if (tag < 0)
			return routingPrefix() + " <incomplete>";
		return String.format("%s trace tag=%08x auth=%08x flags=%02x snr_hops=%d",
				routingPrefix(), tag, authCode, flags, snrData.length);
	}
}
