package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device when a login attempt succeeds, carrying
 * permission flags, the server's 6-byte key prefix, and (for firmware v7+) a
 * timestamp, ACL bits, and firmware version level.
 */
public class LoginSuccessPush extends ResponseFrame {

	/**
	 * Server-reported permission flags: bit 0 – is_admin (1 = admin access granted)
	 * Other bits are server-defined.
	 */
	final int permissions;
	/**
	 * First 6 bytes of the server's public key; used as response key for matching.
	 */
	final byte[] prefix6;
	/** Unix epoch timestamp reflected from the login request (v7+). */
	final long timestamp;
	/**
	 * ACL permission bits reported by the server (v7+). Interpretation is
	 * server-specific.
	 */
	final int acl;
	/**
	 * Firmware version level reported by the server (FIRMWARE_VER_LEVEL); 0 for
	 * pre-v7 servers, advanced since.
	 */
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
		if (fwVersionLevel > 0) {
			return String.format("PUSH_LOGIN_SUCCESS ver=%d admin=%d prefix=%s timestamp=%s ACL=%d", fwVersionLevel,
					permissions, MeshcoreUtils.hex(prefix6), MeshcoreUtils.formatMeshcoreTs(timestamp), acl);
		} else {
			return String.format("PUSH_LOGIN_SUCCESS ver=? permissions=%d prefix=%s", permissions,
					MeshcoreUtils.hex(prefix6));
		}
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(prefix6);
	}
}
