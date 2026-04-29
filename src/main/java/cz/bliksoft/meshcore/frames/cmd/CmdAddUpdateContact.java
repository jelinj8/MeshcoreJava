package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ContactFlags;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that adds a new contact or updates an existing one on the
 * device. No response is expected (the device does not send an explicit
 * acknowledgement for this command).
 */
public class CmdAddUpdateContact extends CommandFrame {

	final byte[] pubkey;
	final AdvertType type;
	final byte flags;
	final int outPathLen;
	final byte[] outPath;
	final String name;
	final long advertTS;

	Double lat;
	Double lon;
	Long lastMod;

	/**
	 * @param pubkey     the 32-byte public key identifying the contact
	 * @param type       the advertisement type of the contact
	 * @param flags      raw contact flags bitmask, see
	 *                   {@link cz.bliksoft.meshcore.frames.FrameConstants.ContactFlags}
	 * @param outPathLen the number of hops in the outbound path
	 * @param outPath    the byte array containing the outbound routing path
	 * @param name       the display name of the contact (max 32 characters)
	 * @param advertTS   the advertisement timestamp in seconds
	 * @param lat        the latitude of the contact, or {@code null} if not
	 *                   available
	 * @param lon        the longitude of the contact, or {@code null} if not
	 *                   available
	 * @param lastMod    the last-modified timestamp in seconds, or {@code null} if
	 *                   not available
	 */
	public CmdAddUpdateContact(byte[] pubkey, AdvertType type, byte flags, int outPathLen, byte[] outPath, String name,
			long advertTS, Double lat, Double lon, Long lastMod) {
		this.rawBytes = null;
		this.pubkey = pubkey;
		this.type = type;
		this.flags = flags;
		this.outPathLen = outPathLen;
		this.outPath = outPath;
		this.name = name;
		this.advertTS = advertTS;
		this.lat = lat;
		this.lon = lon;
		this.lastMod = lastMod;
	}

	/**
	 * Convenience constructor that accepts {@link ContactFlags} varargs instead of
	 * a raw byte.
	 *
	 * @param pubkey       the 32-byte public key identifying the contact
	 * @param type         the advertisement type of the contact
	 * @param outPathLen   the number of hops in the outbound path
	 * @param outPath      the byte array containing the outbound routing path
	 * @param name         the display name of the contact (max 32 characters)
	 * @param advertTS     the advertisement timestamp in seconds
	 * @param lat          the latitude of the contact, or {@code null} if not
	 *                     available
	 * @param lon          the longitude of the contact, or {@code null} if not
	 *                     available
	 * @param lastMod      the last-modified timestamp in seconds, or {@code null}
	 *                     if not available
	 * @param contactFlags zero or more {@link ContactFlags} values to encode into
	 *                     the flags byte
	 */
	public CmdAddUpdateContact(byte[] pubkey, AdvertType type, int outPathLen, byte[] outPath, String name,
			long advertTS, Double lat, Double lon, Long lastMod, ContactFlags... contactFlags) {
		this.rawBytes = null;
		this.pubkey = pubkey;
		this.type = type;
		this.flags = ContactFlags.encode(contactFlags);
		this.outPathLen = outPathLen;
		this.outPath = outPath;
		this.name = name;
		this.advertTS = advertTS;
		this.lat = lat;
		this.lon = lon;
		this.lastMod = lastMod;
	}

	private final byte[] rawBytes; // null when built from fields

	/**
	 * @param rawBytes raw frame bytes received from the device; the frame type byte
	 *                 is overwritten with {@code CMD_ADD_UPDATE_CONTACT}
	 */
	public CmdAddUpdateContact(byte[] rawBytes) {
		this.rawBytes = java.util.Arrays.copyOf(rawBytes, rawBytes.length);
		this.rawBytes[0] = CommandFrameType.CMD_ADD_UPDATE_CONTACT.code();
		// null-init required finals
		this.pubkey = null;
		this.type = null;
		this.flags = 0;
		this.outPathLen = 0;
		this.outPath = null;
		this.name = null;
		this.advertTS = 0;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_ADD_UPDATE_CONTACT;
	}

	@Override
	public byte[] getBytes() {
		if (rawBytes != null)
			return rawBytes;

		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(pubkey);
		bb.put(type.code());
		bb.put(flags);
		bb.put((byte) outPathLen);
		bb.put(outPath, Settings.MAX_PATH_SIZE);
		bb.putFixed(name, 32);
		bb.putUInt32LE(advertTS);
		if (lat != null || lon != null || lastMod != null) {
			if (lat == null)
				lat = 0.0;
			if (lon == null)
				lon = 0.0;
			bb.putInt32LE((int) Math.round(lat * 1000000));
			bb.putInt32LE((int) Math.round(lon * 1000000));

			if (lastMod != null)
				bb.putUInt32LE(lastMod);
		}

		return bb.toArray();
	}

}
