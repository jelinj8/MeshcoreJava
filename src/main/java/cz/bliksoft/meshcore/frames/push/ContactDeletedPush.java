package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame sent by the device when a contact has been removed from its
 * contact list, carrying the public key of the deleted contact.
 */
public class ContactDeletedPush extends ContactFrameGroup {

	final byte[] pubkey;

	/**
	 * @return full public key of the contact that was deleted
	 */
	public byte[] getPubkey() {
		return pubkey;
	}

	public ContactDeletedPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_CONTACT_DELETED;
	}

	@Override
	public String toString() {
		return String.format("CONTACT_DELETED_PUSH pubkey=%s", MeshcoreUtils.hex(pubkey));
	}
}
