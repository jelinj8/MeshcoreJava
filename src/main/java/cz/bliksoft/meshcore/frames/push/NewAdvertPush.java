package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class NewAdvertPush extends ResponseFrame {

	final byte[] pubkey;

	public byte[] getPubkey() {
		return pubkey;
	}

	public NewAdvertPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_NEW_ADVERT;
	}

	@Override
	public String toString() {
		return String.format("PUSH_NEW_ADVERT %s", MeshcoreUtils.hex(pubkey));
	}
}
