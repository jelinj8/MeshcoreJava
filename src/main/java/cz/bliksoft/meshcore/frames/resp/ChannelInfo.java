package cz.bliksoft.meshcore.frames.resp;

import java.util.Arrays;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
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

	public ChannelInfo(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		id = br.readUnsignedByte();
		name = br.readFixedCString(32);
		pubkey = br.readBytes(16); // only 128-bit supported for now
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
