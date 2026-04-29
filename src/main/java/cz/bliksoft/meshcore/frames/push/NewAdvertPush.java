package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.ContactFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.cmd.CmdAddUpdateContact;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Sent when a previously-unknown contact is first seen, or when the firmware
 * detects that a contact's info has changed. Wire format is identical to
 * {@link cz.bliksoft.meshcore.frames.resp.Contact} (RESP_CONTACT).
 *
 * @see AdvertPush for the re-advert notification (pubkey only)
 */
public class NewAdvertPush extends ContactFrameGroup {

	final byte[] pubkey;
	final AdvertType type;
	final byte flags;
	/** Bytes per path-hash entry: 1-3 normally; 4 means OUT_PATH_UNKNOWN (0xFF). */
	final int hashLength;
	/** Number of hops in the stored outbound path. */
	final int pathLength;
	final byte[] outPath;
	final String name;
	/** Unix epoch seconds of last received advert from this contact. */
	final long advertTS;
	final Double lat;
	final Double lon;
	/** Unix epoch seconds of last modification; used for incremental sync. */
	final long lastMod;

	/**
	 * @return full public key of the contact
	 */
	public byte[] getPubkey() {
		return pubkey;
	}

	/**
	 * @return advert type (e.g. ROUTER, CLIENT)
	 */
	public AdvertType getType() {
		return type;
	}

	/**
	 * @return {@code true} if the given contact flag bit is set in this advert
	 */
	public boolean hasFlag(ContactFlags flag) {
		return (flags & flag.mask()) != 0;
	}

	/** Bytes per path-hash entry: 1, 2, or 3. */
	public int getHashLength() {
		return hashLength;
	}

	/** Number of hops in the stored outbound path. */
	public int getPathLength() {
		return pathLength;
	}

	/** True when an outbound path is known (false when OUT_PATH_UNKNOWN / 0xFF). */
	public boolean isPathKnown() {
		return hashLength <= 3;
	}

	/**
	 * Raw encoded path-length byte, as used in OTA packets and
	 * CMD_ADD_UPDATE_CONTACT.
	 */
	public int getOutPathEncoded() {
		return ((hashLength - 1) << 6) | pathLength;
	}

	/**
	 * @return raw encoded outbound path bytes (length = pathLength * hashLength)
	 */
	public byte[] getOutPath() {
		return outPath;
	}

	/**
	 * @return display name of the contact (up to 32 characters)
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Unix epoch seconds of the last received advert from this contact
	 */
	public long getAdvertTS() {
		return advertTS;
	}

	/**
	 * @return latitude in decimal degrees, or {@code null} if not provided
	 */
	public Double getLat() {
		return lat;
	}

	/**
	 * @return longitude in decimal degrees, or {@code null} if not provided
	 */
	public Double getLon() {
		return lon;
	}

	/**
	 * @return Unix epoch seconds of the last modification, used for incremental
	 *         sync
	 */
	public long getLastMod() {
		return lastMod;
	}

	public NewAdvertPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes(Settings.PUBKEY_SIZE);
		type = AdvertType.fromByte(br.readByte());
		flags = br.readByte();
		int outPathRaw = br.readUnsignedByte();
		hashLength = (outPathRaw >> 6) + 1;
		pathLength = outPathRaw & 0x3F;
		outPath = br.readBytes(Settings.MAX_PATH_SIZE);
		name = br.readFixedCString(32);
		advertTS = br.readUInt32LE();
		lat = br.readInt32LE() / 1000000.0;
		lon = br.readInt32LE() / 1000000.0;
		lastMod = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_NEW_ADVERT;
	}

	@Override
	public String toString() {
		return String.format(
				"PUSH_NEW_ADVERT name=%s pubkey=%s type=%s favourite=%b advertTS=%s lat=%.5f lon=%.5f lastMod=%s hops=%d path=%s",
				name, MeshcoreUtils.hex(pubkey), type, hasFlag(ContactFlags.FAVOURITE),
				MeshcoreUtils.formatMeshcoreTs(advertTS), lat, lon, MeshcoreUtils.formatMeshcoreTs(lastMod), pathLength,
				isPathKnown() && pathLength > 0 ? MeshcoreUtils.hex(outPath, hashLength, "-") : "unknown");
	}

	public CmdAddUpdateContact getCmdAddUpdateContact() {
		return new CmdAddUpdateContact(getBytes());
	}
}
