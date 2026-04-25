package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * ACK payload: ack_crc(4).
 *
 * <p>
 * Stored as a raw 4-byte array (same type and byte order as
 * {@link cz.bliksoft.meshcore.frames.resp.Sent#getAckIdOrTag()} and
 * {@link cz.bliksoft.meshcore.frames.push.SendConfirmedPush#getTag()}).
 */
public class OtaAckFrame extends OtaFrame {

	/**
	 * 4-byte ACK tag; matches {@code Sent.getAckIdOrTag()} of the original send.
	 * Null if truncated.
	 */
	public final byte[] ackCrc;

	OtaAckFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path,
			byte[] payloadBytes) {
		super(source, route, OtaPayloadType.ACK, ver, tc0, tc1, hashSize, path, payloadBytes);
		ByteReader br = new ByteReader(payloadBytes);
		ackCrc = br.remaining() >= 4 ? br.readBytes(4) : null;
	}

	@Override
	public String toString() {
		return String.format("%s ack_crc=%s", routingPrefix(), ackCrc != null ? MeshcoreUtils.hex(ackCrc) : "??");
	}
}
