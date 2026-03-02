package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class LoginSuccessPush extends ResponseFrame {

	final int permissions;
	final byte[] prefix6;
	final long timestamp;
	final int acl;
	final int fwVersionLevel;

	public LoginSuccessPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		this.permissions = br.readUnsignedByte();
		this.prefix6 = br.readBytes(6);
		if (data.length >= 14) {
			timestamp = br.readUInt32LE();
			acl = br.readUnsignedByte();
			fwVersionLevel = br.readUnsignedByte();
		} else {
			timestamp = 0;
			acl = 0;
			fwVersionLevel = 6; // advanced since v7
		}

	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_LOGIN_SUCCESS;
	}

	@Override
	public String toString() {
		if (fwVersionLevel > 6) {
			return String.format("PUSH_LOGIN_SUCCESS ver%d permissions=%d prefix=%s timestamp=%s ACL=%d",
					fwVersionLevel, permissions, MeshcoreUtils.hex(prefix6), MeshcoreUtils.formatMeshcoreTs(timestamp),
					acl);
		} else {
			return String.format("PUSH_LOGIN_SUCCESS ver<7 permissions=%d prefix=%s", permissions,
					MeshcoreUtils.hex(prefix6));
		}
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
