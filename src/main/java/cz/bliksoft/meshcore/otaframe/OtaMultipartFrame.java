package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;

/**
 * MULTIPART payload: byte[0] = (remaining&lt;&lt;4) | inner_type, then inner payload bytes.
 */
public class OtaMultipartFrame extends OtaFrame {

	/** Number of remaining multipart fragments. */
	public final int remaining;
	/** Payload type of the inner (reassembled) packet. */
	public final OtaPayloadType innerType;

	OtaMultipartFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, OtaPayloadType.MULTIPART, ver, tc0, tc1, hashSize, path, payloadBytes);
		if (payloadBytes.length >= 1) {
			int b = payloadBytes[0] & 0xFF;
			remaining = b >> 4;
			innerType = OtaPayloadType.fromByte((byte) (b & 0x0F));
		} else {
			remaining = -1;
			innerType = OtaPayloadType.RAW_CUSTOM;
		}
	}

	@Override
	public String toString() {
		return String.format("%s multipart remaining=%d inner=%s", routingPrefix(), remaining, innerType);
	}
}
