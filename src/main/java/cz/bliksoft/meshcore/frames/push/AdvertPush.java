package cz.bliksoft.meshcore.frames.push;

import java.time.Instant;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class AdvertPush extends ResponseFrame {

	final byte[] pubkey;
	final AdvertType type;
	final byte flags;
	final int outPathLen;
	final byte[] outPath;
	final String contactName;
	final long lastAdvert;
	final double lat;
	final double lon;
	final long lastMod;

	public byte[] getPubkey() {
		return pubkey;
	}

	public AdvertPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes();
		if (data.length > 33) {
			this.type = AdvertType.fromByte(br.readByte());
			this.flags = br.readByte();
			this.outPathLen = br.readUnsignedByte();
			this.outPath = br.readBytes(64);
			this.contactName = br.readFixedCString(32);
			this.lastAdvert = br.readUInt32LE();
			this.lat = br.readInt32LE() / 1000000.0;
			this.lon = br.readInt32LE() / 1000000.0;
			this.lastMod = br.readUInt32LE();
		} else {
			this.type = AdvertType.ADV_TYPE_NONE;
			this.flags = 0x00;
			this.outPathLen = 0;
			this.outPath = new byte[64];
			this.contactName = "?";
			this.lastAdvert = Instant.now().getEpochSecond();
			this.lat = 0;
			this.lon = 0;
			this.lastMod = 0l;
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_ADVERT;
	}

	@Override
	public String toString() {
		if (type == AdvertType.ADV_TYPE_NONE) {
			Contact c = companion.getContact(pubkey);
			return String.format("PUSH_ADVERT pubkey=%s [%s]", MeshcoreUtils.hex(pubkey),
					c != null ? c.getName() : "?");
		} else
			return String.format(
					"PUSH_ADVERT pubkey=%s type=%s flags=%d outPathLen=%d outPath=%s contactName=%s lastAdvert=%s lat=%.5f lon=%.5f lastMod=%s",
					MeshcoreUtils.hex(pubkey), type, flags, outPathLen, MeshcoreUtils.hex(outPath), contactName,
					MeshcoreUtils.formatMeshcoreTs(lastAdvert), lat, lon, MeshcoreUtils.formatMeshcoreTs(lastMod));
	}
}
