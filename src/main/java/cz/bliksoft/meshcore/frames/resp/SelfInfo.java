package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertLocPolicy;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.TelemetryModeFlags;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class SelfInfo extends ResponseFrame {

	public AdvertType getAdvertType() {
		return advertType;
	}

	public int getTxPowerDbm() {
		return txPowerDbm;
	}

	public int getMaxLoraPowerDbm() {
		return maxLoraPowerDbm;
	}

	public byte[] getPubkey() {
		return pubkey;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public boolean getMultiAcks() {
		return multiAcks;
	}

	public AdvertLocPolicy getAdvertLocPolicy() {
		return advertLocPolicy;
	}

	public boolean isTelemetryModeBaseFav() {
		return telemetryModeBaseFav;
	}

	public boolean isTelemetryModeBaseEn() {
		return telemetryModeBaseEn;
	}

	public boolean isTelemetryModeLocFav() {
		return telemetryModeLocFav;
	}

	public boolean isTelemetryModeLocEn() {
		return telemetryModeLocEn;
	}

	public boolean isTelemetryModeEnvFav() {
		return telemetryModeEnvFav;
	}

	public boolean isTelemetryModeEnvEn() {
		return telemetryModeEnvEn;
	}

	public boolean isManualAddContacts() {
		return manualAddContacts;
	}

	/**
	 * 
	 * @return freq (Hz)
	 */
	public long getFreq() {
		return freq;
	}

	/**
	 * 
	 * @return bandwidth (Hz)
	 */
	public long getBw() {
		return bw;
	}

	public int getSf() {
		return sf;
	}

	public int getCr() {
		return cr;
	}

	public String getNodeName() {
		return nodeName;
	}

	final AdvertType advertType;
	final int txPowerDbm;
	final int maxLoraPowerDbm;
	final byte[] pubkey;
	final double lat;
	final double lon;
	final boolean multiAcks;
	final AdvertLocPolicy advertLocPolicy;

	final boolean telemetryModeBaseFav;
	final boolean telemetryModeBaseEn;
	final boolean telemetryModeLocFav;
	final boolean telemetryModeLocEn;
	final boolean telemetryModeEnvFav;
	final boolean telemetryModeEnvEn;

	final boolean manualAddContacts;

	/**
	 * (Hz)
	 */
	final long freq;

	/**
	 * (Hz)
	 */
	final long bw;
	final int sf;
	final int cr;
	final String nodeName;

	public SelfInfo(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		advertType = AdvertType.fromByte(br.readByte());
		txPowerDbm = br.readUnsignedByte();
		maxLoraPowerDbm = br.readUnsignedByte();
		pubkey = br.readBytes(Settings.PUBKEY_SIZE);
		lat = br.readInt32LE() / 1000000.0;
		lon = br.readInt32LE() / 1000000.0;

		byte multiAcksByte = br.readByte();
		multiAcks = (companion.getProtocolVersion() >= 7) && (multiAcksByte != 0);

		byte advertLocPolicyByte = br.readByte();
		advertLocPolicy = (companion.getProtocolVersion() >= 7) ? AdvertLocPolicy.fromByte(advertLocPolicyByte)
				: AdvertLocPolicy.ADVERT_LOC_NONE;

		byte telemetryMode = br.readByte();
		if (companion.getProtocolVersion() >= 5) {
			telemetryModeEnvEn = (telemetryMode & TelemetryModeFlags.ENV_ENABLED.mask()) != 0;
			telemetryModeEnvFav = (telemetryMode & TelemetryModeFlags.ENV_FAVORITES_ONLY.mask()) != 0;
			telemetryModeLocEn = (telemetryMode & TelemetryModeFlags.LOC_ENABLED.mask()) != 0;
			telemetryModeLocFav = (telemetryMode & TelemetryModeFlags.LOC_FAVORITES_ONLY.mask()) != 0;
			telemetryModeBaseEn = (telemetryMode & TelemetryModeFlags.BASE_ENABLED.mask()) != 0;
			telemetryModeBaseFav = (telemetryMode & TelemetryModeFlags.BASE_FAVORITES_ONLY.mask()) != 0;
		} else {
			telemetryModeEnvEn = telemetryModeEnvFav = false;
			telemetryModeLocEn = telemetryModeLocFav = false;
			telemetryModeBaseEn = telemetryModeBaseFav = false;
		}

		manualAddContacts = (br.readByte() & 0x01) == 1;

		freq = br.readUInt32LE() * 1000;
		bw = br.readUInt32LE() * 1000;
		sf = br.readUnsignedByte();
		cr = br.readUnsignedByte();

		nodeName = br.readFixedCString(100);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SELF_INFO;
	}

	@Override
	public String toString() {
		return String.format(
				"RESP_SELF_INFO nodeName=%s advertType=%s txPowerBdm=%d maxLoRaPowerDbm=%d pubkey32=%s lat=%f lon=%f multiAck=%b advertLocPolicy=%s telemetryModeEnvEn=%b telemetryModeEnvFav=%b telemetryModeLocEn=%b telemetryModeLocFav=%b telemetryModeBaseEn=%b telemetryModeBaseFav=%b manualAddContacts=%s freq=%d bw=%d sf=%d cr=%d",
				nodeName, advertType, txPowerDbm, maxLoraPowerDbm, MeshcoreUtils.hex(pubkey), lat, lon, multiAcks,
				advertLocPolicy, telemetryModeEnvEn, telemetryModeEnvFav, telemetryModeLocEn, telemetryModeLocFav,
				telemetryModeBaseEn, telemetryModeBaseFav, manualAddContacts, freq, bw, sf, cr);
	}
}
