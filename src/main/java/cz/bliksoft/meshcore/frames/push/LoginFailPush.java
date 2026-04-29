package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device when a login attempt fails, carrying the 6-byte
 * node prefix that identifies which login request was rejected.
 */
public class LoginFailPush extends ResponseFrame {

	/**
	 * @return reserved byte from the wire frame (currently unused)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return first 6 bytes of the target node's public key, used as the response
	 *         key for matching
	 */
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
