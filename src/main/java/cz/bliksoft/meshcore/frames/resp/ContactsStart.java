package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;

public class ContactsStart extends ContactFrameGroup {

	final long contactsCount;

	public long getContactsCount() {
		return contactsCount;
	}

	public ContactsStart(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		contactsCount = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_CONTACTS_START;
	}

	@Override
	public String toString() {
		return String.format("RESP_CONTACTS_START totalCount=%d", contactsCount);
	}

}
