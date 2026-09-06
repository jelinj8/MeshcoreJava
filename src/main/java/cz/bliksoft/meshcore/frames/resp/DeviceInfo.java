package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdDeviceQuery} containing
 * firmware version, protocol capabilities, hardware identity, and BLE PIN of
 * the device.
 */
public class DeviceInfo extends ResponseFrame {

	/**
	 * @return companion protocol version code sent by the firmware (C++
	 *         {@code FIRMWARE_VER_CODE})
	 */
	public int getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * @return maximum number of contacts the device can store, or {@code -1} if
	 *         unknown (protocol &lt; 3)
	 */
	public int getMaxContacts() {
		return maxContacts;
	}

	/**
	 * @return maximum number of group channels supported, or {@code -1} if unknown
	 *         (protocol &lt; 3)
	 */
	public int getMaxGroupChannels() {
		return maxGroupChannels;
	}

	/**
	 * @return 6-digit BLE PIN; value {@code 1000000} indicates all-zeroes (no PIN
	 *         set)
	 */
	public long getBlePIN() {
		return blePIN;
	}

	/**
	 * @return firmware build date string as reported by the device
	 */
	public String getFirmwareBuildDate() {
		return firmwareBuildDate;
	}

	/**
	 * @return device manufacturer string as reported by the firmware
	 */
	public String getDeviceManufacturer() {
		return deviceManufacturer;
	}

	/**
	 * @return human-readable firmware version string (e.g. {@code "v1.14.0"})
	 */
	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	/**
	 * @return {@code true} if the device supports client-repeat mode (protocol
	 *         version 9+)
	 */
	public boolean isClientRepeat() {
		return clientRepeat;
	}

	/**
	 * Path-hash mode (0–2), present from firmware v1.14+ (protocol version 10+).
	 */
	public int getPathHashMode() {
		return pathHashMode;
	}

	/**
	 * Internal companion-protocol version code sent by the firmware (C++
	 * {@code FIRMWARE_VER_CODE}). This is NOT the human-readable release string —
	 * see {@link #firmwareVersion} for that. Known mapping:
	 * <ul>
	 * <li>7 → v1.7.2+</li>
	 * <li>8 → v1.10.0+</li>
	 * <li>9 → v1.12.0+</li>
	 * <li>10 → v1.14.0+</li>
	 * <li>11 → v1.15.0+</li>
	 * <li>12 → adds a second {@code CMD_SET_FLOOD_SCOPE_KEY} variant (force
	 * unscoped sending); no new {@code RESP_CODE_DEVICE_INFO} fields</li>
	 * <li>13 → v1.16.0+; {@code CMD_SEND_ANON_REQ} allows non-contact recipients;
	 * adds {@code CMD_SEND_RAW_PACKET}; no new {@code RESP_CODE_DEVICE_INFO}
	 * fields. Confirmed unchanged through v1.17.1 - the v1.17.0→v1.17.1 firmware
	 * diff only touches a FEM radio gain prefs bugfix, not the companion
	 * protocol.</li>
	 * </ul>
	 */
	final int protocolVersion;
	final int maxContacts;
	final int maxGroupChannels;

	/**
	 * 6digit, 1000000 for zeroes
	 */
	final long blePIN;

	final String firmwareBuildDate;
	final String deviceManufacturer;
	final String firmwareVersion;
	final boolean clientRepeat;
	final int pathHashMode;

	public DeviceInfo(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		// (1) companion protocol version (C++ FIRMWARE_VER_CODE)
		protocolVersion = br.readUnsignedByte();
		if (protocolVersion >= 3) {
			// (1) firmware sends MAX_CONTACTS / 2; multiply by 2 to get the actual contact
			// limit (v3+)
			maxContacts = br.readUnsignedByte() * 2;
			// (1) max group channels (v3+)
			maxGroupChannels = br.readUnsignedByte();
		} else {
			maxContacts = -1;
			maxGroupChannels = -1;
		}

		// (4) BLE pin
		blePIN = br.readUInt32LE();

		// (12) firmware build date
		firmwareBuildDate = br.readFixedCString(12);

		// (40)n device manufacturer
		deviceManufacturer = br.readFixedCString(40);

		// (20)n firmware version
		firmwareVersion = br.readFixedCString(20);

		if (protocolVersion >= 9) {
			// (1) client repeat
			clientRepeat = br.readUnsignedByte() != 0;
		} else {
			clientRepeat = false;
		}

		if (protocolVersion >= 10) {
			pathHashMode = br.readUnsignedByte();
		} else {
			pathHashMode = 0;
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_DEVICE_INFO;
	}

	@Override
	public String toString() {
		return String.format(
				"RESP_DEVICE_INFO protocolVersion=%d maxContacts=%d maxGroupChannels=%d blePIN=%06d fwBuildDate=%s manufacturer=%s fwVersion=%s clientRepeat=%b pathHashMode=%d",
				protocolVersion, maxContacts, maxGroupChannels, blePIN, firmwareBuildDate, deviceManufacturer,
				firmwareVersion, clientRepeat, pathHashMode);
	}
}
