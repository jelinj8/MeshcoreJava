package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;

/**
 * CONTROL payload: single control/discovery byte.
 */
public class OtaControlFrame extends OtaFrame {

	public final int controlByte;

	OtaControlFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, OtaPayloadType.CONTROL, ver, tc0, tc1, hashSize, path, payloadBytes);
		controlByte = payloadBytes.length >= 1 ? payloadBytes[0] & 0xFF : -1;
	}

	@Override
	public String toString() {
		return controlByte >= 0
				? String.format("%s control=0x%02x", routingPrefix(), controlByte)
				: routingPrefix() + " <incomplete>";
	}
}
