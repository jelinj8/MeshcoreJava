package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.ContactFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.cmd.CmdAddUpdateContact;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class Contact extends ContactFrameGroup {

	public byte[] getPubkey() {
		return pubkey;
	}

	public AdvertType getType() {
		return type;
	}

	/**
	 * Returns true if the given {@link ContactFlags} bit is set in this contact's
	 * flags byte.
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

	public byte[] getOutPath() {
		return outPath;
	}

	public String getName() {
		return name;
	}

	public long getAdvertTS() {
		return advertTS;
	}

	public Double getLat() {
		return lat;
	}

	public Double getLon() {
		return lon;
	}

	public long getLastMod() {
		return lastMod;
	}

	/**
	 * Returns {@code true} when this contact is saved on the device. Returns
	 * {@code false} for contacts received via {@code PUSH_NEW_ADVERT} (seen over
	 * the air but not yet added); such contacts can be imported via
	 * {@link #getCmdAddUpdateContact()}.
	 */
	public boolean isSaved() {
		return saved;
	}

	final byte[] pubkey;
	final AdvertType type;
	/** Bitmask — see {@link ContactFlags} for individual bit definitions. */
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

	public boolean saved;

	public Contact(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);

		Byte fType = br.readByte();

		if (fType == FrameConstants.ResponseFrameType.PUSH_NEW_ADVERT.code()) {
			// convert frame to a contact for saving (identical structure, different type).
			saved = false;
			data[0] = FrameConstants.ResponseFrameType.RESP_CONTACT.code();
		} else {
			saved = true;
		}

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
		return ResponseFrameType.RESP_CONTACT;
	}

	@Override
	public String toString() {
		if (!isPathKnown() || pathLength == 0) {
			return String.format(
					"RESP_CONTACT name=%s pubkey32=%s type=%s flags=%d advertTS=%s lat=%f lon=%f lastMod=%s path=%s",
					name, MeshcoreUtils.hex(pubkey), type, flags, MeshcoreUtils.formatMeshcoreTs(advertTS), lat, lon,
					MeshcoreUtils.formatMeshcoreTs(lastMod), isPathKnown() ? "direct" : "unknown");
		} else {
			return String.format(
					"RESP_CONTACT name=%s pubkey32=%s type=%s favourite=%b flags=%d advertTS=%s lat=%f lon=%f lastMod=%s hops=%d path=%s",
					name, MeshcoreUtils.hex(pubkey), type, hasFlag(ContactFlags.FAVOURITE), flags,
					MeshcoreUtils.formatMeshcoreTs(advertTS), lat, lon, MeshcoreUtils.formatMeshcoreTs(lastMod),
					pathLength, MeshcoreUtils.hex(outPath, hashLength, "-"));
		}
	}

	public CmdAddUpdateContact getCmdAddUpdateContact() {
		return new CmdAddUpdateContact(getBytes());
	}

}
