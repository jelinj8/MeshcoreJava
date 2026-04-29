package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device when the stored routing path to a known contact
 * has been updated, carrying the contact's public key so the cached path can be
 * refreshed.
 */
public class PathUpdatedPush extends ContactFrameGroup {

	final byte[] pubkey;

	/**
	 * @return full public key of the contact whose path was updated
	 */
	public byte[] getPubkey() {
		return pubkey;
	}

	public PathUpdatedPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_PATH_UPDATED;
	}

	@Override
	public String toString() {
		return String.format("PUSH_PATH_UPDATED %s", MeshcoreUtils.hex(pubkey));
	}
}
