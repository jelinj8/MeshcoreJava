package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * TRACE payload: trace_tag(4 LE) auth_code(4 LE) flags(1) snr_data(...).
 */
public class OtaTraceFrame extends OtaFrame {

	public final long tag;
	public final long authCode;
	public final int flags;
	/** SNR readings collected at each hop. */
	public final byte[] snrData;

	OtaTraceFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path,
			byte[] payloadBytes) {
		super(source, route, OtaPayloadType.TRACE, ver, tc0, tc1, hashSize, path, payloadBytes);
		ByteReader br = new ByteReader(payloadBytes);
		if (br.remaining() >= 9) {
			tag = br.readUInt32LE();
			authCode = br.readUInt32LE();
			flags = br.readUnsignedByte();
			snrData = br.readBytes();
		} else {
			tag = authCode = -1;
			flags = -1;
			snrData = new byte[0];
		}
	}

	@Override
	public String toString() {
		if (tag < 0)
			return routingPrefix() + " <incomplete>";
		return String.format("%s trace tag=%08x auth=%08x flags=%02x snr_hops=%d", routingPrefix(), tag, authCode,
				flags, snrData.length);
	}
}
