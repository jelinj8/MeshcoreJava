package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class LoginFailPush extends ResponseFrame {

	public byte getReserved() {
		return reserved;
	}

	public byte[] getPrefix6() {
		return prefix6;
	}

	final byte reserved;
	final byte[] prefix6;

	public LoginFailPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		prefix6 = br.readBytes(6);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_LOGIN_FAIL;
	}

	@Override
	public String toString() {
		return String.format("PUSH_LOGIN_FAIL reserved=%d prefix=%s", reserved, MeshcoreUtils.hex(prefix6));
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
