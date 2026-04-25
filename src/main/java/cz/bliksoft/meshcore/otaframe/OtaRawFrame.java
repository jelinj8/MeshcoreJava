package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Fallback for unknown or RAW_CUSTOM payload types. The raw payload bytes are
 * available via the inherited {@link OtaFrame#payloadBytes} field.
 */
public class OtaRawFrame extends OtaFrame {

	OtaRawFrame(MeshcoreCompanion source, OtaRouteType route, OtaPayloadType payloadType, int ver, int tc0, int tc1,
			int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
	}

	@Override
	public String toString() {
		return routingPrefix() + " payload=" + MeshcoreUtils.hex(payloadBytes);
	}
}
