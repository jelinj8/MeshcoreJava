package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
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

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_ADD_UPDATE_CONTACT;
	}

	@Override
	public byte[] getBytes() {
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
