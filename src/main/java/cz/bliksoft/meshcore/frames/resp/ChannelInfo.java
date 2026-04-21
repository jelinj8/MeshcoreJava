package cz.bliksoft.meshcore.frames.resp;

import java.util.Arrays;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class ChannelInfo extends ResponseFrame {

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public byte[] getPubkey() {
		return pubkey;
	}

	final int id;
	final String name;
	final byte[] pubkey;
	private int channelHash = -1;

	public ChannelInfo(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		id = br.readUnsignedByte();
		name = br.readFixedCString(32);
		pubkey = br.readBytes(16); // only 128-bit supported for now
	}

	public int getChannelHash() {
		if (channelHash == -1) {
			try {
				java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
				channelHash = md.digest(pubkey)[0] & 0xFF;
			} catch (java.security.NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
		return channelHash;
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_CHANNEL_INFO;
	}

	@Override
	public String toString() {
		if (Arrays.equals(Settings.PUBLIC_GROUP_PSK, pubkey)) {
			return String.format("RESP_CHANNEL_INFO id=%d name=%s key=PUBLIC", id, name);
		} else {
			return String.format("RESP_CHANNEL_INFO id=%d name=%s key=%s", id, name, MeshcoreUtils.hex(pubkey));
		}
	}
}
