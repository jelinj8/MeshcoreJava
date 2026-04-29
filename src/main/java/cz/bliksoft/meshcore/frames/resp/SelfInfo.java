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

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdDeviceQuery} carrying
 * the device's own identity and radio configuration: node name, public key,
 * LoRa parameters, GPS coordinates, and telemetry policy flags.
 */
public class SelfInfo extends ResponseFrame {

	/**
	 * @return advertisement type used by this node
	 */
	public AdvertType getAdvertType() {
		return advertType;
	}

	/**
	 * @return configured TX power in dBm
	 */
	public int getTxPowerDbm() {
		return txPowerDbm;
	}

	/**
	 * @return maximum LoRa TX power supported by the hardware in dBm
	 */
	public int getMaxLoraPowerDbm() {
		return maxLoraPowerDbm;
	}

	/**
	 * @return the node's public key bytes
	 */
	public byte[] getPubkey() {
		return pubkey;
	}

	/**
	 * @return latitude in decimal degrees (positive = North)
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * @return longitude in decimal degrees (positive = East)
	 */
	public double getLon() {
		return lon;
	}

	/**
	 * @return {@code true} if multi-ACK mode is enabled (protocol version 7+)
	 */
	public boolean getMultiAcks() {
		return multiAcks;
	}

	/**
	 * @return the advertisement location sharing policy (protocol version 7+)
	 */
	public AdvertLocPolicy getAdvertLocPolicy() {
		return advertLocPolicy;
	}

	/**
	 * @return {@code true} if base telemetry is restricted to favourite contacts
	 *         only
	 */
	public boolean isTelemetryModeBaseFav() {
		return telemetryModeBaseFav;
	}

	/**
	 * @return {@code true} if base telemetry sharing is enabled
	 */
	public boolean isTelemetryModeBaseEn() {
		return telemetryModeBaseEn;
	}

	/**
	 * @return {@code true} if location telemetry is restricted to favourite
	 *         contacts only
	 */
	public boolean isTelemetryModeLocFav() {
		return telemetryModeLocFav;
	}

	/**
	 * @return {@code true} if location telemetry sharing is enabled
	 */
	public boolean isTelemetryModeLocEn() {
		return telemetryModeLocEn;
	}

	/**
	 * @return {@code true} if environment telemetry is restricted to favourite
	 *         contacts only
	 */
	public boolean isTelemetryModeEnvFav() {
		return telemetryModeEnvFav;
	}

	/**
	 * @return {@code true} if environment telemetry sharing is enabled
	 */
	public boolean isTelemetryModeEnvEn() {
		return telemetryModeEnvEn;
	}

	/**
	 * @return {@code true} if new contacts must be added manually (auto-add is
	 *         disabled)
	 */
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

	/**
	 * @return LoRa spreading factor
	 */
	public int getSf() {
		return sf;
	}

	/**
	 * @return LoRa coding rate
	 */
	public int getCr() {
		return cr;
	}

	/**
	 * @return human-readable name of this node as configured on the device
	 */
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
		multiAcks = (companion.getConfig().getProtocolVersion() >= 7) && (multiAcksByte != 0);

		byte advertLocPolicyByte = br.readByte();
		advertLocPolicy = (companion.getConfig().getProtocolVersion() >= 7)
				? AdvertLocPolicy.fromByte(advertLocPolicyByte)
				: AdvertLocPolicy.ADVERT_LOC_NONE;

		// telemetryMode byte layout (v5+): bits [5:4]=env_mode, bits [3:2]=loc_mode,
		// bits [1:0]=base_mode
		// Each 2-bit field: 0=disabled, 1=allow_favorites_only, 2=allow_all
		byte telemetryMode = br.readByte();
		if (companion.getConfig().getProtocolVersion() >= 5) {
			telemetryModeEnvEn = (telemetryMode
					& (TelemetryModeFlags.ENV_ALLOW_FAVORITES.mask() | TelemetryModeFlags.ENV_ALLOW_ALL.mask())) != 0;
			telemetryModeEnvFav = (telemetryMode & TelemetryModeFlags.ENV_ALLOW_FAVORITES.mask()) != 0;
			telemetryModeLocEn = (telemetryMode
					& (TelemetryModeFlags.LOC_ALLOW_FAVORITES.mask() | TelemetryModeFlags.LOC_ALLOW_ALL.mask())) != 0;
			telemetryModeLocFav = (telemetryMode & TelemetryModeFlags.LOC_ALLOW_FAVORITES.mask()) != 0;
			telemetryModeBaseEn = (telemetryMode
					& (TelemetryModeFlags.BASE_ALLOW_FAVORITES.mask() | TelemetryModeFlags.BASE_ALLOW_ALL.mask())) != 0;
			telemetryModeBaseFav = (telemetryMode & TelemetryModeFlags.BASE_ALLOW_FAVORITES.mask()) != 0;
		} else {
			telemetryModeEnvEn = telemetryModeEnvFav = false;
			telemetryModeLocEn = telemetryModeLocFav = false;
			telemetryModeBaseEn = telemetryModeBaseFav = false;
		}

		manualAddContacts = (br.readByte() & 0x01) == 1;

		freq = br.readUInt32LE() * 1000; // firmware sends kHz → store as Hz
		bw = br.readUInt32LE(); // firmware sends Hz → store as Hz
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
