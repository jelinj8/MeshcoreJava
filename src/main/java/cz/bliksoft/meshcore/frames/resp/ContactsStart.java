package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetContacts} that opens
 * a contact-list synchronisation sequence and reports the total number of
 * contacts to follow.
 */
public class ContactsStart extends ContactFrameGroup {

	final long contactsCount;

	/**
	 * @return total number of {@link Contact} frames that will follow in this
	 *         synchronisation sequence
	 */
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
