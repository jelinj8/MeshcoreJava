package cz.bliksoft.meshcore.otaframe;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * ADVERT payload: pub_key(32) timestamp(4 LE) signature(64) app_data(...).
 *
 * <p>
 * app_data layout:
 * 
 * <pre>
 *   flags(1)              adv_type[3:0], HAS_LATLON[4], HAS_FEAT1[5], HAS_FEAT2[6], HAS_NAME[7]
 *   [lat(4 LE) lon(4 LE)] when HAS_LATLON
 *   [feat1(2 LE)]         when HAS_FEAT1
 *   [feat2(2 LE)]         when HAS_FEAT2
 *   [name(rest, UTF-8)]   when HAS_NAME
 * </pre>
 */
public class OtaAdvertFrame extends OtaFrame {

	public final byte[] pubKey;
	public final long timestamp;
	/** Advert type nibble (0=NONE, 1=CHAT, 2=REPEATER, 3=ROOM, 4=SENSOR). */
	public final int advertType;
	/** Latitude in degrees, or {@code null} when not present. */
	public final Double lat;
	/** Longitude in degrees, or {@code null} when not present. */
	public final Double lon;
	public final Integer feat1;
	public final Integer feat2;
	/** Node name, or {@code null} when not present. */
	public final String name;

	OtaAdvertFrame(MeshcoreCompanion source, OtaRouteType route, int ver, int tc0, int tc1, int hashSize, byte[] path,
			byte[] payloadBytes) {
		super(source, route, OtaPayloadType.ADVERT, ver, tc0, tc1, hashSize, path, payloadBytes);

		int minLen = Settings.PUBKEY_SIZE + 4 + Settings.SIGNATURE_SIZE;
		if (payloadBytes.length < minLen) {
			pubKey = payloadBytes.length >= Settings.PUBKEY_SIZE ? Arrays.copyOf(payloadBytes, Settings.PUBKEY_SIZE)
					: new byte[0];
			timestamp = -1;
			advertType = -1;
			lat = null;
			lon = null;
			feat1 = null;
			feat2 = null;
			name = null;
			return;
		}

		ByteReader br = new ByteReader(payloadBytes);
		pubKey = br.readBytes(Settings.PUBKEY_SIZE);
		timestamp = br.readUInt32LE();
		br.skip(Settings.SIGNATURE_SIZE);

		if (br.remaining() == 0) {
			advertType = -1;
			lat = null;
			lon = null;
			feat1 = null;
			feat2 = null;
			name = null;
			return;
		}

		int flags = br.readUnsignedByte();
		advertType = flags & 0x0F;

		Double tmpLat = null, tmpLon = null;
		if ((flags & 0x10) != 0 && br.remaining() >= 8) {
			tmpLat = br.readInt32LE() / 1_000_000.0;
			tmpLon = br.readInt32LE() / 1_000_000.0;
		}
		lat = tmpLat;
		lon = tmpLon;

		Integer tmpFeat1 = null;
		if ((flags & 0x20) != 0 && br.remaining() >= 2) {
			tmpFeat1 = br.readUInt16LE();
		}
		feat1 = tmpFeat1;

		Integer tmpFeat2 = null;
		if ((flags & 0x40) != 0 && br.remaining() >= 2) {
			tmpFeat2 = br.readUInt16LE();
		}
		feat2 = tmpFeat2;

		name = ((flags & 0x80) != 0 && br.remaining() > 0) ? new String(br.readBytes(), StandardCharsets.UTF_8) : null;
	}

	private static String advertTypeName(int type) {
		switch (type) {
		case 0:
			return "NONE";
		case 1:
			return "CHAT";
		case 2:
			return "REPEATER";
		case 3:
			return "ROOM";
		case 4:
			return "SENSOR";
		default:
			return "TYPE_" + type;
		}
	}

	@Override
	public String toString() {
		if (timestamp < 0)
			return routingPrefix() + " advert=<incomplete " + payloadBytes.length + "B>";
		StringBuilder sb = new StringBuilder(routingPrefix());
		sb.append(" pub=").append(MeshcoreUtils.hexPrefix6(pubKey));
		sb.append(" ts=").append(MeshcoreUtils.formatMeshcoreTs(timestamp));
		if (advertType >= 0)
			sb.append(" adv_type=").append(advertTypeName(advertType));
		if (lat != null)
			sb.append(String.format(" lat=%.6f lon=%.6f", lat, lon));
		if (feat1 != null)
			sb.append(String.format(" feat1=%04x", feat1));
		if (feat2 != null)
			sb.append(String.format(" feat2=%04x", feat2));
		if (name != null)
			sb.append(" name=\"").append(name).append("\"");
		return sb.toString();
	}
}
