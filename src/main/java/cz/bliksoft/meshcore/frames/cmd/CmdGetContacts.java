package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that requests the device's contact list, optionally filtered to
 * contacts updated after a given timestamp. Expects a
 * {@code RESP_CONTACTS_START} response followed by a stream of contact entries.
 */
public class CmdGetContacts extends CommandFrame {

	final Long since;

	/**
	 * contacts request
	 *
	 * @param since optional (nullable) timestamp to filter newer contacts
	 */
	public CmdGetContacts(Long since) {
		this.since = since;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_CONTACTS;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		if (since != null)
			bb.putUInt32LE(since);
		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_CONTACTS_START.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
