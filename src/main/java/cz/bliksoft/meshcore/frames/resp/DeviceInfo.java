package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class DeviceInfo extends ResponseFrame {

	public int getFirmwareVersionCode() {
		return firmwareVersionCode;
	}

	public int getMaxContacts() {
		return maxContacts;
	}

	public int getMaxGroupChannels() {
		return maxGroupChannels;
	}

	public long getBlePIN() {
		return blePIN;
	}

	public String getFirmwareBuildDate() {
		return firmwareBuildDate;
	}

	public String getDeviceManufacturer() {
		return deviceManufacturer;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public boolean isClientRepeat() {
		return clientRepeat;
	}

	/** Path-hash mode (0–2), present from firmware v10+. */
	public int getPathHashMode() {
		return pathHashMode;
	}

	final int firmwareVersionCode;
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

		// (1) firmware version code
		firmwareVersionCode = br.readUnsignedByte();
		if (firmwareVersionCode >= 3) {
			// (1) firmware sends MAX_CONTACTS / 2; multiply by 2 to get the actual contact limit (v3+)
			maxContacts = br.readUnsignedByte();
			// (1) max group channels (v3+)
			maxGroupChannels = br.readUnsignedByte();
		} else {
			maxContacts = -1;
			maxGroupChannels = -1;
		}
		source.setMaxChannels(maxGroupChannels);
		source.setMaxContacts(maxContacts);

		// (4) BLE pin
		blePIN = br.readUInt32LE();

		// (12) firmware build date
		firmwareBuildDate = br.readFixedCString(12);

		// (40)n device manufacturer
		deviceManufacturer = br.readFixedCString(40);

		// (20)n firmware version
		firmwareVersion = br.readFixedCString(20);

		if (firmwareVersionCode >= 9) {
			// (1) client repeat
			clientRepeat = br.readUnsignedByte() != 0;
		} else {
			clientRepeat = false;
		}

		if (firmwareVersionCode >= 10) {
			pathHashMode = br.readUnsignedByte();
		} else {
			pathHashMode = 0;
		}

		companion.setProtocolVersion(firmwareVersionCode);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_DEVICE_INFO;
	}

	@Override
	public String toString() {
		return String.format(
				"RESP_DEVICE_INFO fw:%d maxContacts=%d maxGroupChannels=%d blePIN=%06d fwBuildDate=%s manufacturer=%s fwVersion=%s clientRepeat=%b pathHashMode=%d",
				firmwareVersionCode, maxContacts, maxGroupChannels, blePIN, firmwareBuildDate, deviceManufacturer,
				firmwareVersion, clientRepeat, pathHashMode);
	}
}
