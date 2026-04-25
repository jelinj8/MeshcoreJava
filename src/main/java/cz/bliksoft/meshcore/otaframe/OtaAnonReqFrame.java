package cz.bliksoft.meshcore.otaframe;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Anonymous request payload: dest_hash(1) ephemeral_pub_key(32) MAC(2)
 * ciphertext.
 */
public class OtaAnonReqFrame extends OtaFrame {

	public final int destHash;
	public final byte[] ephemeralPubKey;
	public final byte[] macAndCipher;

	OtaAnonReqFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path,
			byte[] payloadBytes) {
		super(source, route, OtaPayloadType.ANON_REQ, ver, tc0, tc1, hashSize, path, payloadBytes);
		ByteReader br = new ByteReader(payloadBytes);
		if (br.remaining() >= 1 + Settings.PUBKEY_SIZE + 2) {
			destHash = br.readUnsignedByte();
			ephemeralPubKey = br.readBytes(Settings.PUBKEY_SIZE);
			macAndCipher = br.readBytes();
		} else {
			destHash = -1;
			ephemeralPubKey = new byte[0];
			macAndCipher = new byte[0];
		}
	}

	@Override
	public String toString() {
		if (destHash < 0)
			return routingPrefix() + " <incomplete>";
		int encLen = macAndCipher.length - 2;
		return String.format("%s anon_req dest=%02x ephem=%s enc=%dB", routingPrefix(), destHash,
				MeshcoreUtils.hexPrefix6(ephemeralPubKey), encLen);
	}
}
