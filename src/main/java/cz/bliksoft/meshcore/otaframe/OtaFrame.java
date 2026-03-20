package cz.bliksoft.meshcore.otaframe;

import java.util.Arrays;
import java.util.List;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Parsed over-the-air packet. Contains the routing header fields common to all
 * packet types. The specific payload is represented by the concrete subclass
 * returned by {@link #parse(byte[])}.
 *
 * <p>
 * Wire layout (from Packet.h):
 * 
 * <pre>
 *   header(1)  = ver[7:6] | payloadType[5:2] | routeType[1:0]
 *   [tc0(2LE) tc1(2LE)]   — present when routeType has transport codes
 *   pathLenEncoded(1)     = hashSize[7:6]-1 | hashCount[5:0]
 *   path(hashSize*hashCount)
 *   payload(...)
 * </pre>
 */
public abstract class OtaFrame {

	protected final MeshcoreCompanion source;

	public final OtaRouteType route;
	public final OtaPayloadType payloadType;
	public final int ver;
	/** First transport code (region key), or -1 when not present. */
	public final int tc0;
	/** Second transport code (region key), or -1 when not present. */
	public final int tc1;
	/** Bytes per path-hash entry (1–3). */
	public final int hashSize;
	/** Concatenated path hashes (length = hashSize * hopCount). */
	public final byte[] path;
	/** Raw payload bytes after the routing header. */
	public final byte[] payloadBytes;

	protected OtaFrame(MeshcoreCompanion source, OtaRouteType route, OtaPayloadType payloadType, int ver, int tc0,
			int tc1, int hashSize, byte[] path, byte[] payloadBytes) {
		this.source = source;
		this.route = route;
		this.payloadType = payloadType;
		this.ver = ver;
		this.tc0 = tc0;
		this.tc1 = tc1;
		this.hashSize = hashSize;
		this.path = path;
		this.payloadBytes = payloadBytes;
	}

	/**
	 * Parse a raw OTA packet byte array and return the appropriate typed subclass.
	 * Returns {@code null} if the input is null, empty, or unparseable.
	 */
	public static OtaFrame parse(MeshcoreCompanion source, byte[] raw) {
		if (raw == null || raw.length < 1)
			return null;
		try {
			return parseInternal(source, raw);
		} catch (Exception e) {
			return null;
		}
	}

	private static OtaFrame parseInternal(MeshcoreCompanion source, byte[] raw) {
		int i = 0;
		int header = raw[i++] & 0xFF;
		OtaRouteType route = OtaRouteType.fromByte((byte) (header & 0x03));
		OtaPayloadType payloadType = OtaPayloadType.fromByte((byte) ((header >> 2) & 0x0F));
		int ver = (header >> 6) & 0x03;

		int tc0 = -1, tc1 = -1;
		if (route.hasTransportCodes() && i + 3 < raw.length) {
			tc0 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
			tc1 = (raw[i] & 0xFF) | ((raw[i + 1] & 0xFF) << 8);
			i += 2;
		}

		int pathLenEncoded = i < raw.length ? raw[i++] & 0xFF : 0;
		int hashSize = (pathLenEncoded >> 6) + 1;
		int hashCount = pathLenEncoded & 0x3F;
		int pathEnd = i + hashCount * hashSize;
		byte[] path = pathEnd <= raw.length ? Arrays.copyOfRange(raw, i, pathEnd) : new byte[0];
		i = pathEnd;

		byte[] payloadBytes = i < raw.length ? Arrays.copyOfRange(raw, i, raw.length) : new byte[0];

		switch (payloadType) {
		case REQ:
		case RESPONSE:
		case TXT_MSG:
		case PATH:
			return new OtaUnicastFrame(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
		case ACK:
			return new OtaAckFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		case ADVERT:
			return new OtaAdvertFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		case GRP_TXT:
		case GRP_DATA:
			return new OtaGroupFrame(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
		case ANON_REQ:
			return new OtaAnonReqFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		case TRACE:
			return new OtaTraceFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		case MULTIPART:
			return new OtaMultipartFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		case CONTROL:
			return new OtaControlFrame(source, route, ver, tc0, tc1, hashSize, path, payloadBytes);
		default:
			return new OtaRawFrame(source, route, payloadType, ver, tc0, tc1, hashSize, path, payloadBytes);
		}
	}

	/**
	 * Shared routing prefix for all subclass {@code toString()} implementations.
	 */
	protected String routingPrefix() {
		String tcStr = tc0 >= 0 ? String.format(" tc=%04x/%04x", tc0, tc1) : "";
		if (LogRXDataPush.isTranslatePath()) {
			return String.format("route=%s type=%s ver=%d%s hops=%d %s", route, payloadType, ver, tcStr,
					path.length / hashSize, (path.length > 0 ? "\n" : "path=") + getDetailedPath());
		} else {
			String pathStr = path.length > 0 ? MeshcoreUtils.hex(path, hashSize, "-") : "direct";
			return String.format("route=%s type=%s ver=%d%s hops=%d path=%s", route, payloadType, ver, tcStr,
					path.length / hashSize, pathStr);
		}
	}

	/** Returns true if the packet arrived with no intermediate hops (path is empty). */
	public boolean isDirect() {
		return path.length == 0;
	}

	/** Returns the number of intermediate hops recorded in the path (0 = direct). */
	public int getHopCount() {
		return path.length / hashSize;
	}

	public String getDetailedPath() {
		if (path.length > 0) {
			StringBuilder sb = new StringBuilder();
			ByteReader br = new ByteReader(path);
			for (int i = 0; i < path.length / hashSize; i++) {
				byte[] prefix = br.readBytes(hashSize);
				List<Contact> contacts = source.findContacts(prefix, null);
				if (contacts.size() == 0) {
					if (i > 0)
						sb.append("\n");
					sb.append(String.format("%s: ??", MeshcoreUtils.hex(prefix)));
				} else {
					boolean first = true;
					for (Contact c : contacts) {
						if (first) {
							first = false;
							if (i > 0)
								sb.append("\n");
							sb.append(String.format("%s: %s %s", MeshcoreUtils.hex(prefix),
									MeshcoreUtils.hex(c.getPubkey(), 3), c.getName()));
						} else {
							sb.append(String.format("\n%" + (hashSize * 2) + "s  %s %s", "",
									MeshcoreUtils.hex(c.getPubkey(), 3), c.getName()));
						}
					}
				}
			}
			sb.append("\n");
			return sb.toString();
		} else
			return "direct";

	}

	@Override
	public String toString() {
		return String.format("OtaFrame %s", routingPrefix());
	}
}
