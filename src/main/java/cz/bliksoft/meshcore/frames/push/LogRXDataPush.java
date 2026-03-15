package cz.bliksoft.meshcore.frames.push;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.OtaConstants;
import cz.bliksoft.meshcore.frames.ResponseFrame;
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
		return String.format("%s snr:%.2f rssi:%d %s", ResponseFrameType.PUSH_LOG_RX_DATA, snr4 / 4.0, rssi,
				decodeRawPacket(rawData));
	}

	// OTA wire constants (from Mesh.h / Packet.h)
	private static final int PUB_KEY_SIZE = Settings.PUBKEY_SIZE;
	private static final int SIGNATURE_SIZE = Settings.SIGNATURE_SIZE;
	private static final int CIPHER_MAC_SIZE = 2; // 2-byte MAC prefix before encrypted payload

	public static String decodeRawPacket(byte[] raw) {
		if (raw == null || raw.length == 0)
			return "<empty>";
		try {
			return decodeRawPacketInternal(raw);
		} catch (Exception e) {
			return "decode_err raw=" + MeshcoreUtils.hex(raw);
		}
	}

	private static String decodeRawPacketInternal(byte[] raw) {
		int i = 0;
		int header = raw[i++] & 0xFF;
		OtaConstants.OtaRouteType route = OtaConstants.OtaRouteType.fromByte((byte) (header & 0x03));
		OtaConstants.OtaPayloadType payload = OtaConstants.OtaPayloadType.fromByte((byte) ((header >> 2) & 0x0F));
		int ver = (header >> 6) & 0x03;

		// transport codes (region keys) — present for TRANSPORT_FLOOD /
		// TRANSPORT_DIRECT
		String tcStr = "";
		if (route.hasTransportCodes()) {
			int tc0 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
			int tc1 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
			tcStr = String.format(" tc=%04x/%04x", tc0, tc1);
		}

		int pathLenEncoded = raw[i++] & 0xFF;
		int hashSize = (pathLenEncoded >> 6) + 1;
		int hashCount = pathLenEncoded & 0x3F;
		byte[] path = Arrays.copyOfRange(raw, i, i + hashCount * hashSize);
		i += hashCount * hashSize;

		byte[] payloadData = Arrays.copyOfRange(raw, i, raw.length);

		String pathStr = path.length > 0 ? MeshcoreUtils.hex(path, hashSize, "-") : "direct";
		return String.format("route=%s type=%s ver=%d%s path=%s %s", route, payload, ver, tcStr, pathStr,
				decodePayload(payload, payloadData));
	}

	private static String decodePayload(OtaConstants.OtaPayloadType type, byte[] data) {
		if (data == null || data.length == 0)
			return "<no payload>";
		switch (type) {
		case REQ:
		case RESPONSE:
		case TXT_MSG:
		case PATH:
			return decodeEncryptedUnicast(data);
		case ACK:
			return decodeAck(data);
		case ADVERT:
			return decodeAdvert(data);
		case GRP_TXT:
		case GRP_DATA:
			return decodeEncryptedGroup(type, data);
		case ANON_REQ:
			return decodeAnonReq(data);
		case TRACE:
			return decodeTrace(data);
		case MULTIPART:
			return decodeMultipart(data);
		case CONTROL:
			return String.format("control=0x%02x", data[0] & 0xFF);
		default:
			return "payload=" + MeshcoreUtils.hex(data);
		}
	}

	/** REQ / RESPONSE / TXT_MSG / PATH: dest_hash(1) src_hash(1) MAC(2) enc_data */
	private static String decodeEncryptedUnicast(byte[] data) {
		if (data.length < 4)
			return "payload=" + MeshcoreUtils.hex(data);
		int dest = data[0] & 0xFF;
		int src = data[1] & 0xFF;
		int encLen = data.length - 2 - CIPHER_MAC_SIZE;
		return String.format("dest=%02x src=%02x enc=%dB", dest, src, encLen);
	}

	/** ACK: ack_crc(4 bytes LE) */
	private static String decodeAck(byte[] data) {
		if (data.length < 4)
			return "payload=" + MeshcoreUtils.hex(data);
		long crc = (data[0] & 0xFFL) | ((data[1] & 0xFFL) << 8) | ((data[2] & 0xFFL) << 16) | ((data[3] & 0xFFL) << 24);
		return String.format("ack_crc=%08x", crc);
	}

	/**
	 * ADVERT: pub_key(32) timestamp(4 LE) signature(64) app_data(...) app_data:
	 * flags(1) [lat(4)+lon(4)] [feat1(2)] [feat2(2)] [name(rest, UTF-8)]
	 */
	private static String decodeAdvert(byte[] data) {
		int minLen = PUB_KEY_SIZE + 4 + SIGNATURE_SIZE;
		if (data.length < minLen)
			return "advert=<incomplete " + data.length + "B>";

		int i = 0;
		byte[] pubKey = Arrays.copyOfRange(data, i, i + PUB_KEY_SIZE);
		i += PUB_KEY_SIZE;
		long ts = (data[i] & 0xFFL) | ((data[i + 1] & 0xFFL) << 8) | ((data[i + 2] & 0xFFL) << 16)
				| ((data[i + 3] & 0xFFL) << 24);
		i += 4;
		i += SIGNATURE_SIZE; // skip signature — not interesting for logging

		StringBuilder sb = new StringBuilder();
		sb.append("advert pub=").append(MeshcoreUtils.hexPrefix6(pubKey));
		sb.append(" ts=").append(MeshcoreUtils.formatMeshcoreTs(ts));

		if (i >= data.length)
			return sb.toString(); // no app_data

		int flags = data[i++] & 0xFF;
		int advType = flags & 0x0F;
		sb.append(" adv_type=").append(advertTypeName(advType));

		if ((flags & 0x10) != 0 && i + 8 <= data.length) { // HAS_LATLON
			int lat = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8) | ((data[i + 2] & 0xFF) << 16)
					| (data[i + 3] << 24);
			i += 4;
			int lon = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8) | ((data[i + 2] & 0xFF) << 16)
					| (data[i + 3] << 24);
			i += 4;
			sb.append(String.format(" lat=%.6f lon=%.6f", lat / 1_000_000.0, lon / 1_000_000.0));
		}
		if ((flags & 0x20) != 0 && i + 2 <= data.length) { // HAS_FEAT1
			int feat1 = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8);
			i += 2;
			sb.append(String.format(" feat1=%04x", feat1));
		}
		if ((flags & 0x40) != 0 && i + 2 <= data.length) { // HAS_FEAT2
			int feat2 = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8);
			i += 2;
			sb.append(String.format(" feat2=%04x", feat2));
		}
		if ((flags & 0x80) != 0 && i < data.length) { // HAS_NAME — rest of app_data is the name (UTF-8, no null
														// terminator)
			String name = new String(data, i, data.length - i, StandardCharsets.UTF_8);
			sb.append(" name=\"").append(name).append("\"");
		}

		return sb.toString();
	}

	/** GRP_TXT / GRP_DATA: channel_hash(1) MAC(2) enc_data */
	private static String decodeEncryptedGroup(OtaConstants.OtaPayloadType type, byte[] data) {
		if (data.length < 1 + CIPHER_MAC_SIZE)
			return "payload=" + MeshcoreUtils.hex(data);
		int ch = data[0] & 0xFF;
		int encLen = data.length - 1 - CIPHER_MAC_SIZE;
		String label = type == OtaConstants.OtaPayloadType.GRP_TXT ? "grp_txt" : "grp_data";
		return String.format("%s ch=%02x enc=%dB", label, ch, encLen);
	}

	/** ANON_REQ: dest_hash(1) ephemeral_pub_key(32) MAC(2) enc_data */
	private static String decodeAnonReq(byte[] data) {
		if (data.length < 1 + PUB_KEY_SIZE + CIPHER_MAC_SIZE)
			return "payload=" + MeshcoreUtils.hex(data);
		int dest = data[0] & 0xFF;
		byte[] ephKey = Arrays.copyOfRange(data, 1, 1 + PUB_KEY_SIZE);
		int encLen = data.length - 1 - PUB_KEY_SIZE - CIPHER_MAC_SIZE;
		return String.format("anon_req dest=%02x ephem=%s enc=%dB", dest, MeshcoreUtils.hexPrefix6(ephKey), encLen);
	}

	/** TRACE: trace_tag(4 LE) auth_code(4 LE) flags(1) snr_data(...) */
	private static String decodeTrace(byte[] data) {
		if (data.length < 9)
			return "payload=" + MeshcoreUtils.hex(data);
		int i = 0;
		long tag = (data[i] & 0xFFL) | ((data[i + 1] & 0xFFL) << 8) | ((data[i + 2] & 0xFFL) << 16)
				| ((data[i + 3] & 0xFFL) << 24);
		i += 4;
		long authCode = (data[i] & 0xFFL) | ((data[i + 1] & 0xFFL) << 8) | ((data[i + 2] & 0xFFL) << 16)
				| ((data[i + 3] & 0xFFL) << 24);
		i += 4;
		int flags = data[i++] & 0xFF;
		int snrLen = data.length - i;
		return String.format("trace tag=%08x auth=%08x flags=%02x snr_hops=%d", tag, authCode, flags, snrLen);
	}

	/** MULTIPART: byte[0] = (remaining<<4)|inner_type, then inner payload */
	private static String decodeMultipart(byte[] data) {
		if (data.length < 1)
			return "payload=" + MeshcoreUtils.hex(data);
		int b = data[0] & 0xFF;
		int remaining = b >> 4;
		int innerType = b & 0x0F;
		OtaConstants.OtaPayloadType inner = OtaConstants.OtaPayloadType.fromByte((byte) innerType);
		return String.format("multipart remaining=%d inner=%s", remaining, inner);
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
}
