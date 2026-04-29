package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;

/**
 * Push frame sent by the device when its contact storage is full and no new
 * contacts can be added until existing ones are removed.
 */
public class ContactsFullPush extends ContactFrameGroup {

	public ContactsFullPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_CONTACTS_FULL;
	}

	@Override
	public String toString() {
		return "PUSH_CONTACTS_FULL";
	}
}
