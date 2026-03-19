package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ContactFlags;
import cz.bliksoft.meshcore.utils.ByteBuilder;

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
