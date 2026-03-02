package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
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

	public byte getFlags() {
		return flags;
	}

	public int getOutPathLen() {
		return outPathLen;
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

	final byte[] pubkey;
	final AdvertType type;
	final byte flags;
	final int outPathLen;
	final byte[] outPath;
	final String name;
	final long advertTS;

	final Double lat;
	final Double lon;
	final long lastMod;

	public Contact(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes(Settings.PUBKEY_SIZE);
		type = AdvertType.fromByte(br.readByte());
		flags = br.readByte();
		outPathLen = br.readUnsignedByte();
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
		if (outPathLen == 0 || outPathLen == 255) {
			return String.format(
					"RESP_CONTACT name=%s pubkey32=%s type=%s flags=%d advertTS=%s lat=%f lon=%f lastMod=%s outPathLen=%d",
					name, MeshcoreUtils.hex(pubkey), type, flags, MeshcoreUtils.formatMeshcoreTs(advertTS), lat, lon,
					MeshcoreUtils.formatMeshcoreTs(lastMod), outPathLen);
		} else {
			return String.format(
					"RESP_CONTACT name=%s pubkey32=%s type=%s flags=%d advertTS=%s lat=%f lon=%f lastMod=%s outPathLen=%d outPath=%s",
					name, MeshcoreUtils.hex(pubkey), type, flags, MeshcoreUtils.formatMeshcoreTs(advertTS), lat, lon,
					MeshcoreUtils.formatMeshcoreTs(lastMod), outPathLen, MeshcoreUtils.hex(outPath, outPathLen));
		}
	}
}
