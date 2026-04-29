package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that requests contact information for a given public key.
 * Expects a {@link cz.bliksoft.meshcore.frames.resp.Contact} response, or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} on failure.
 */
public class CmdGetContactByKey extends CommandFrame {

	final byte[] pubkey;

	/**
	 * @param pubkey the 32-byte public key identifying the contact to retrieve
	 */
	public CmdGetContactByKey(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_CONTACT_BY_KEY;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(pubkey);

		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_CONTACT.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
