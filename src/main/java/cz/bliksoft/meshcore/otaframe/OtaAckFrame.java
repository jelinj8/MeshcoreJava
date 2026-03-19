package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;

/**
 * ACK payload: ack_crc(4 LE).
 */
public class OtaAckFrame extends OtaFrame {

	/** CRC used to match the acknowledged message. */
	public final long ackCrc;

	OtaAckFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path, byte[] payloadBytes) {
		super(source, route, OtaPayloadType.ACK, ver, tc0, tc1, hashSize, path, payloadBytes);
		ackCrc = payloadBytes.length >= 4
				? (payloadBytes[0] & 0xFFL) | ((payloadBytes[1] & 0xFFL) << 8)
				  | ((payloadBytes[2] & 0xFFL) << 16) | ((payloadBytes[3] & 0xFFL) << 24)
				: -1;
	}

	@Override
	public String toString() {
		return String.format("%s ack_crc=%08x", routingPrefix(), ackCrc);
	}
}
