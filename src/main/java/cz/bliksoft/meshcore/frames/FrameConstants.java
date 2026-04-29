package cz.bliksoft.meshcore.frames;

import java.util.HashMap;
import java.util.Map;

public class FrameConstants {
	private FrameConstants() {
	}

	public enum CommandFrameType {
		CMD_APP_START((byte) 1), // 0x01 / 1
		CMD_SEND_TXT_MSG((byte) 2), // 0x02 / 2
		CMD_SEND_CHANNEL_TXT_MSG((byte) 3), // 0x03 / 3

		/** with optional 'since' (for efficient sync) */
		CMD_GET_CONTACTS((byte) 4), // 0x04 / 4

		CMD_GET_DEVICE_TIME((byte) 5), // 0x05 / 5
		CMD_SET_DEVICE_TIME((byte) 6), // 0x06 / 6
		CMD_SEND_SELF_ADVERT((byte) 7), // 0x07 / 7
		CMD_SET_ADVERT_NAME((byte) 8), // 0x08 / 8
		CMD_ADD_UPDATE_CONTACT((byte) 9), // 0x09 / 9
		CMD_SYNC_NEXT_MESSAGE((byte) 10), // 0x0A / 10
		CMD_SET_RADIO_PARAMS((byte) 11), // 0x0B / 11
		CMD_SET_RADIO_TX_POWER((byte) 12), // 0x0C / 12
		CMD_RESET_PATH((byte) 13), // 0x0D / 13
		CMD_SET_ADVERT_LATLON((byte) 14), // 0x0E / 14
		CMD_REMOVE_CONTACT((byte) 15), // 0x0F / 15
		CMD_SHARE_CONTACT((byte) 16), // 0x10 / 16
		CMD_EXPORT_CONTACT((byte) 17), // 0x11 / 17
		CMD_IMPORT_CONTACT((byte) 18), // 0x12 / 18
		CMD_REBOOT((byte) 19), // 0x13 / 19

		/** was CMD_GET_BATTERY_VOLTAGE */
		CMD_GET_BATT_AND_STORAGE((byte) 20), // 0x14 / 20

		CMD_SET_TUNING_PARAMS((byte) 21), // 0x15 / 21
		CMD_DEVICE_QEURY((byte) 22), // 0x16 / 22
		CMD_EXPORT_PRIVATE_KEY((byte) 23), // 0x17 / 23
		CMD_IMPORT_PRIVATE_KEY((byte) 24), // 0x18 / 24
		CMD_SEND_RAW_DATA((byte) 25), // 0x19 / 25
		CMD_SEND_LOGIN((byte) 26), // 0x1A / 26
		CMD_SEND_STATUS_REQ((byte) 27), // 0x1B / 27
		CMD_HAS_CONNECTION((byte) 28), // 0x1C / 28

		/** 'Disconnect' */
		CMD_LOGOUT((byte) 29), // 0x1D / 29

		CMD_GET_CONTACT_BY_KEY((byte) 30), // 0x1E / 30
		CMD_GET_CHANNEL((byte) 31), // 0x1F / 31
		CMD_SET_CHANNEL((byte) 32), // 0x20 / 32
		CMD_SIGN_START((byte) 33), // 0x21 / 33
		CMD_SIGN_DATA((byte) 34), // 0x22 / 34
		CMD_SIGN_FINISH((byte) 35), // 0x23 / 35
		CMD_SEND_TRACE_PATH((byte) 36), // 0x24 / 36
		CMD_SET_DEVICE_PIN((byte) 37), // 0x25 / 37
		CMD_SET_OTHER_PARAMS((byte) 38), // 0x26 / 38

		/** can deprecate this */
		CMD_SEND_TELEMETRY_REQ((byte) 39), // 0x27 / 39

		CMD_GET_CUSTOM_VARS((byte) 40), // 0x28 / 40
		CMD_SET_CUSTOM_VAR((byte) 41), // 0x29 / 41
		CMD_GET_ADVERT_PATH((byte) 42), // 0x2A / 42
		CMD_GET_TUNING_PARAMS((byte) 43), // 0x2B / 43

		CMD_SEND_BINARY_REQ((byte) 50), // 0x32 / 50
		CMD_FACTORY_RESET((byte) 51), // 0x33 / 51
		CMD_SEND_PATH_DISCOVERY_REQ((byte) 52), // 0x34 / 52

		/** v1.10+ (protocol 8) */
		CMD_SET_FLOOD_SCOPE((byte) 54), // 0x36 / 54

		/** v1.10+ (protocol 8) */
		CMD_SEND_CONTROL_DATA((byte) 55), // 0x37 / 55

		/** v1.10+ (protocol 8); second byte is stats sub-type */
		CMD_GET_STATS((byte) 56), // 0x38 / 56

		CMD_SEND_ANON_REQ((byte) 57), // 0x39 / 57
		CMD_SET_AUTOADD_CONFIG((byte) 58), // 0x3A / 58
		CMD_GET_AUTOADD_CONFIG((byte) 59), // 0x3B / 59
		CMD_GET_ALLOWED_REPEAT_FREQ((byte) 60), // 0x3C / 60
		CMD_SET_PATH_HASH_MODE((byte) 61), // 0x3D / 61

		/** v1.15+ send binary datagram to a group channel */
		CMD_SEND_CHANNEL_DATA((byte) 62), // 0x3E / 62

		/** v1.15+ set/clear persisted default flood scope (name+key) */
		CMD_SET_DEFAULT_FLOOD_SCOPE((byte) 63), // 0x3F / 63

		/** v1.15+ get persisted default flood scope */
		CMD_GET_DEFAULT_FLOOD_SCOPE((byte) 64); // 0x40 / 64

		private final byte code;

		CommandFrameType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		public int unsigned() {
			return code & 0xFF;
		}

		private static final Map<Byte, CommandFrameType> BY_CODE = new HashMap<>();

		static {
			for (CommandFrameType c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static CommandFrameType fromByte(byte value) {
			return BY_CODE.get(value);
		}
	}

	/** Stats sub-types for CMD_GET_STATS */
	public enum StatsCommandFrameSubtype {

		CORE((byte) 0), // 0x00 / 0
		RADIO((byte) 1), // 0x01 / 1
		PACKETS((byte) 2); // 0x02 / 2

		private final byte code;

		StatsCommandFrameSubtype(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, StatsCommandFrameSubtype> BY_CODE = new HashMap<>();

		static {
			for (StatsCommandFrameSubtype c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static StatsCommandFrameSubtype fromByte(byte value) {
			return BY_CODE.get(value);
		}
	}

	public enum MessageTextType {
		TXT_TYPE_PLAIN((byte) 0), // a plain text message
		TXT_TYPE_CLI_DATA((byte) 1), // a CLI command
		TXT_TYPE_SIGNED_PLAIN((byte) 2); // plain text, signed by sender

		private final byte code;

		MessageTextType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, MessageTextType> BY_CODE = new HashMap<>();

		static {
			for (MessageTextType c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static MessageTextType fromByte(byte value) {
			return BY_CODE.get(value);
		}
	}

	public enum ResponseFrameType {

		RESP_OK((byte) 0), // 0x00 / 0
		RESP_ERR((byte) 1), // 0x01 / 1

		/** first reply to CMD_GET_CONTACTS */
		RESP_CONTACTS_START((byte) 2), // 0x02 / 2

		/** multiple of these (after CMD_GET_CONTACTS) */
		RESP_CONTACT((byte) 3), // 0x03 / 3

		/** last reply to CMD_GET_CONTACTS */
		RESP_END_OF_CONTACTS((byte) 4), // 0x04 / 4

		/** reply to CMD_APP_START */
		RESP_SELF_INFO((byte) 5), // 0x05 / 5

		/** reply to CMD_SEND_TXT_MSG */
		RESP_SENT((byte) 6), // 0x06 / 6

		/** a reply to CMD_SYNC_NEXT_MESSAGE (ver &lt; 3) */
		RESP_CONTACT_MSG_RECV((byte) 7), // 0x07 / 7

		/** a reply to CMD_SYNC_NEXT_MESSAGE (ver &lt; 3) */
		RESP_CHANNEL_MSG_RECV((byte) 8), // 0x08 / 8

		/** a reply to CMD_GET_DEVICE_TIME */
		RESP_CURR_TIME((byte) 9), // 0x09 / 9

		/** a reply to CMD_SYNC_NEXT_MESSAGE */
		RESP_NO_MORE_MESSAGES((byte) 10), // 0x0A / 10

		RESP_EXPORT_CONTACT((byte) 11), // 0x0B / 11

		/** a reply to a CMD_GET_BATT_AND_STORAGE */
		RESP_BATT_AND_STORAGE((byte) 12), // 0x0C / 12

		/** a reply to CMD_DEVICE_QEURY */
		RESP_DEVICE_INFO((byte) 13), // 0x0D / 13

		/** a reply to CMD_EXPORT_PRIVATE_KEY */
		RESP_PRIVATE_KEY((byte) 14), // 0x0E / 14

		RESP_DISABLED((byte) 15), // 0x0F / 15

		/** a reply to CMD_SYNC_NEXT_MESSAGE (ver >= 3) */
		RESP_CONTACT_MSG_RECV_V3((byte) 16), // 0x10 / 16

		/** a reply to CMD_SYNC_NEXT_MESSAGE (ver >= 3) */
		RESP_CHANNEL_MSG_RECV_V3((byte) 17), // 0x11 / 17

		/** a reply to CMD_GET_CHANNEL */
		RESP_CHANNEL_INFO((byte) 18), // 0x12 / 18

		RESP_SIGN_START((byte) 19), // 0x13 / 19
		RESP_SIGNATURE((byte) 20), // 0x14 / 20
		RESP_CUSTOM_VARS((byte) 21), // 0x15 / 21
		RESP_ADVERT_PATH((byte) 22), // 0x16 / 22
		RESP_TUNING_PARAMS((byte) 23), // 0x17 / 23

		/** v1.10+ (protocol 8); second byte is stats sub-type */
		RESP_STATS((byte) 24), // 0x18 / 24

		RESP_AUTOADD_CONFIG((byte) 25), // 0x19 / 25
		RESP_ALLOWED_REPEAT_FREQ((byte) 26), // 0x1A / 26

		/** v1.15+ reply to CMD_SYNC_NEXT_MESSAGE when channel data is queued */
		RESP_CHANNEL_DATA_RECV((byte) 27), // 0x1B / 27

		/** v1.15+ reply to CMD_GET_DEFAULT_FLOOD_SCOPE */
		RESP_DEFAULT_FLOOD_SCOPE((byte) 28), // 0x1C / 28

		// PUSH messages

		PUSH_ADVERT((byte) 0x80), // 0x80 / 128
		PUSH_PATH_UPDATED((byte) 0x81), // 0x81 / 129
		PUSH_SEND_CONFIRMED((byte) 0x82), // 0x82 / 130
		PUSH_MSG_WAITING((byte) 0x83), // 0x83 / 131
		PUSH_RAW_DATA((byte) 0x84), // 0x84 / 132
		PUSH_LOGIN_SUCCESS((byte) 0x85), // 0x85 / 133
		PUSH_LOGIN_FAIL((byte) 0x86), // 0x86 / 134
		PUSH_STATUS_RESPONSE((byte) 0x87), // 0x87 / 135
		PUSH_LOG_RX_DATA((byte) 0x88), // 0x88 / 136
		PUSH_TRACE_DATA((byte) 0x89), // 0x89 / 137
		PUSH_NEW_ADVERT((byte) 0x8A), // 0x8A / 138
		PUSH_TELEMETRY_RESPONSE((byte) 0x8B), // 0x8B / 139
		PUSH_BINARY_RESPONSE((byte) 0x8C), // 0x8C / 140
		PUSH_PATH_DISCOVERY_RESPONSE((byte) 0x8D), // 0x8D / 141

		/** v1.10+ (protocol 8) */
		PUSH_CONTROL_DATA((byte) 0x8E), // 0x8E / 142

		/** used to notify client app of deleted contact when overwriting oldest */
		PUSH_CONTACT_DELETED((byte) 0x8F), // 0x8F / 143

		/** used to notify client app that contacts storage is full */
		PUSH_CONTACTS_FULL((byte) 0x90); // 0x90 / 144

		private final byte code;

		ResponseFrameType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, ResponseFrameType> BY_CODE = new HashMap<>();

		static {
			for (ResponseFrameType c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static ResponseFrameType fromByte(byte value) {
			return BY_CODE.get(value);
		}

	}

	public enum ErrorFrameResultCode {

		UNSUPPORTED_CMD((byte) 1), // 0x01 / 1
		NOT_FOUND((byte) 2), // 0x02 / 2
		TABLE_FULL((byte) 3), // 0x03 / 3
		BAD_STATE((byte) 4), // 0x04 / 4
		FILE_IO_ERROR((byte) 5), // 0x05 / 5
		ILLEGAL_ARG((byte) 6); // 0x06 / 6

		private final byte code;

		ErrorFrameResultCode(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, ErrorFrameResultCode> BY_CODE = new HashMap<>();

		static {
			for (ErrorFrameResultCode c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static ErrorFrameResultCode fromByte(byte value) {
			return BY_CODE.get(value);
		}

	}

	/**
	 * Auto-add config bitmask Bit 0: If set, overwrite oldest non-favourite contact
	 * when contacts file is full Bits 1-4: these indicate which contact types to
	 * auto-add when manual_contact_mode = 0x01
	 */
	public enum AutoAddConfigFlags {

		/** 0x01 - overwrite oldest non-favourite when full */
		OVERWRITE_OLDEST((byte) (1 << 0)), // 0x01 / 1

		/** 0x02 - auto-add Chat (Companion) (ADV_TYPE_CHAT) */
		CHAT((byte) (1 << 1)), // 0x02 / 2

		/** 0x04 - auto-add Repeater (ADV_TYPE_REPEATER) */
		REPEATER((byte) (1 << 2)), // 0x04 / 4

		/** 0x08 - auto-add Room Server (ADV_TYPE_ROOM) */
		ROOM_SERVER((byte) (1 << 3)), // 0x08 / 8

		/** 0x10 - auto-add Sensor (ADV_TYPE_SENSOR) */
		SENSOR((byte) (1 << 4)); // 0x10 / 16

		private final byte mask;

		AutoAddConfigFlags(byte mask) {
			this.mask = mask;
		}

		public byte mask() {
			return mask;
		}

		/** Combines one or more flags into a single byte suitable for the wire. */
		public static byte encode(AutoAddConfigFlags... flags) {
			byte result = 0;
			for (AutoAddConfigFlags f : flags) {
				result |= f.mask;
			}
			return result;
		}
	}

	/**
	 * Telemetry mode flags packed into a single byte by the firmware:
	 * {@code (env_mode << 4) | (loc_mode << 2) | base_mode}
	 * <p>
	 * Each 2-bit field uses the values: 0 = deny, 1 = allow favorites only, 2 =
	 * allow all. The two bits per channel are exposed as separate mask constants:
	 * {@code _ALLOW_FAVORITES} = bit 0 of the field (mode == 1), {@code _ALLOW_ALL}
	 * = bit 1 of the field (mode == 2).
	 * <p>
	 * To check whether a channel is enabled at all (mode != 0):
	 * {@code (byte & (X_ALLOW_FAVORITES.mask() | X_ALLOW_ALL.mask())) != 0}
	 */
	public enum TelemetryModeFlags {

		/** bits[1:0] = 01 — base telemetry allowed for favourite contacts only */
		BASE_ALLOW_FAVORITES((byte) (1 << 0)),
		/** bits[1:0] = 10 — base telemetry allowed for all contacts */
		BASE_ALLOW_ALL((byte) (1 << 1)),
		/** bits[3:2] = 01 — location telemetry allowed for favourite contacts only */
		LOC_ALLOW_FAVORITES((byte) (1 << 2)),
		/** bits[3:2] = 10 — location telemetry allowed for all contacts */
		LOC_ALLOW_ALL((byte) (1 << 3)),
		/**
		 * bits[5:4] = 01 — environment telemetry allowed for favourite contacts only
		 */
		ENV_ALLOW_FAVORITES((byte) (1 << 4)),
		/** bits[5:4] = 10 — environment telemetry allowed for all contacts */
		ENV_ALLOW_ALL((byte) (1 << 5));

		private final byte mask;

		TelemetryModeFlags(byte mask) {
			this.mask = mask;
		}

		public byte mask() {
			return mask;
		}
	}

	/**
	 * Bitmask flags stored in the {@code flags} field of
	 * {@link cz.bliksoft.meshcore.frames.resp.Contact}.
	 * <p>
	 * Bit 0: favourite flag. Bits 1–3: per-contact telemetry permission bits (used
	 * when the corresponding telemetry mode is {@code TELEM_MODE_ALLOW_FLAGS}). The
	 * firmware shifts {@code flags >> 1} to get the permission byte, so bit N+1 of
	 * {@code flags} maps to {@code TELEM_PERM_*} bit N.
	 */
	public enum ContactFlags {

		/** bit 0 — contact is marked as favourite */
		FAVOURITE((byte) 0x01),
		/** bit 1 — allow base (battery/self) telemetry requests from this contact */
		TELEM_PERMISSION_BASE((byte) 0x02),
		/** bit 2 — allow location telemetry requests from this contact */
		TELEM_PERMISSION_LOCATION((byte) 0x04),
		/** bit 3 — allow environment telemetry requests from this contact */
		TELEM_PERMISSION_ENVIRONMENT((byte) 0x08);

		private final byte mask;

		ContactFlags(byte mask) {
			this.mask = mask;
		}

		public byte mask() {
			return mask;
		}

		/** Combines one or more flags into a single byte suitable for the wire. */
		public static byte encode(ContactFlags... flags) {
			byte result = 0;
			for (ContactFlags f : flags) {
				result |= f.mask;
			}
			return result;
		}
	}

	public enum AdvertType {
		ADV_TYPE_NONE((byte) 0), ADV_TYPE_CHAT((byte) 1), ADV_TYPE_REPEATER((byte) 2), ADV_TYPE_ROOM((byte) 3),
		ADV_TYPE_SENSOR((byte) 4);

		private final byte code;

		AdvertType(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, AdvertType> BY_CODE = new HashMap<>();

		static {
			for (AdvertType c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static AdvertType fromByte(byte value) {
			return BY_CODE.get(value);
		}

	}

	public enum AdvertLocPolicy {
		ADVERT_LOC_NONE((byte) 0), ADVERT_LOC_SHARE((byte) 1), ADVERT_LOC_PREFS((byte) 2);

		private final byte code;

		AdvertLocPolicy(byte code) {
			this.code = code;
		}

		public byte code() {
			return code;
		}

		private static final Map<Byte, AdvertLocPolicy> BY_CODE = new HashMap<>();

		static {
			for (AdvertLocPolicy c : values()) {
				BY_CODE.put(c.code, c);
			}
		}

		public static AdvertLocPolicy fromByte(byte value) {
			return BY_CODE.get(value);
		}

	}

	// framing for serial comm
	public static final int SERIAL_FRAME_FROM_RADIO = '>';
	public static final int SERIAL_FRAME_TO_RADIO = '<';

	/** Advert app_data flags byte (AdvertDataHelpers.h). */
	public enum AdvertAppDataFlags {
		TYPE_MASK((byte) 0x0F), HAS_LATLON((byte) 0x10), HAS_FEAT1((byte) 0x20), HAS_FEAT2((byte) 0x40),
		HAS_NAME((byte) 0x80);

		private final byte mask;

		AdvertAppDataFlags(byte mask) {
			this.mask = mask;
		}

		public byte mask() {
			return mask;
		}
	}

}
