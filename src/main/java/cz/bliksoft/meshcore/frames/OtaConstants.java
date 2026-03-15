package cz.bliksoft.meshcore.frames;

import java.util.HashMap;
import java.util.Map;

public class OtaConstants {

	// ---- Raw over-the-air packet header decoding (Packet.h) ----

	/** Route type field: bits[1:0] of the OTA packet header byte. */
	public enum OtaRouteType {
		TRANSPORT_FLOOD((byte) 0x00), // flood + transport codes
		FLOOD((byte) 0x01), // flood, path built up during relay
		DIRECT((byte) 0x02), // direct route, path supplied
		TRANSPORT_DIRECT((byte) 0x03); // direct + transport codes

		private final byte code;

		OtaRouteType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		public boolean isFlood() {
			return this == FLOOD || this == TRANSPORT_FLOOD;
		}

		public boolean isDirect() {
			return this == DIRECT || this == TRANSPORT_DIRECT;
		}

		public boolean hasTransportCodes() {
			return this == TRANSPORT_FLOOD || this == TRANSPORT_DIRECT;
		}

		private static final Map<Byte, OtaRouteType> BY_CODE = new HashMap<>();
		static {
			for (OtaRouteType v : values())
				BY_CODE.put(v.code, v);
		}

		public static OtaRouteType fromByte(byte value) {
			OtaRouteType v = BY_CODE.get(value);
			return v != null ? v : FLOOD;
		}
	}

	/**
	 * Payload type field: bits[5:2] of the OTA packet header byte.
	 * <p>
	 * REQ / RESPONSE / TXT_MSG / PATH payload is encrypted; only dest/src 1-byte
	 * hashes and a 2-byte MAC are visible at the front. ADVERT is signed but not
	 * encrypted.
	 */
	public enum OtaPayloadType {
		REQ((byte) 0x00), // encrypted request (dest_hash, src_hash, MAC, enc_data)
		RESPONSE((byte) 0x01), // encrypted response (dest_hash, src_hash, MAC, enc_data)
		TXT_MSG((byte) 0x02), // encrypted text (dest_hash, src_hash, MAC, enc_data)
		ACK((byte) 0x03), // simple ack
		ADVERT((byte) 0x04), // signed identity broadcast (pub_key, timestamp, signature, app_data)
		GRP_TXT((byte) 0x05), // group text (channel_hash, MAC, enc_data)
		GRP_DATA((byte) 0x06), // group datagram (channel_hash, MAC, enc_data)
		ANON_REQ((byte) 0x07), // anonymous request (dest_hash, ephemeral_pub_key, MAC, enc_data)
		PATH((byte) 0x08), // returned path (dest_hash, src_hash, MAC, enc_data)
		TRACE((byte) 0x09), // path trace, collecting SNR at each hop
		MULTIPART((byte) 0x0A), // one part of a multi-packet message
		CONTROL((byte) 0x0B), // control / discovery
		RAW_CUSTOM((byte) 0x0F); // application-defined, custom encryption

		private final byte code;

		OtaPayloadType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, OtaPayloadType> BY_CODE = new HashMap<>();
		static {
			for (OtaPayloadType v : values())
				BY_CODE.put(v.code, v);
		}

		public static OtaPayloadType fromByte(byte value) {
			return BY_CODE.getOrDefault(value, RAW_CUSTOM);
		}
	}
}