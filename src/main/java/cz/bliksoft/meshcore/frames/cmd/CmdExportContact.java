package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdExportContact extends CommandFrame {

	final byte[] pubkey;

	/**
	 * export contact
	 * 
	 * @param pubkey32 null for 'self'
	 */
	public CmdExportContact(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_EXPORT_CONTACT;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		if (pubkey != null)
			bb.put(pubkey);
		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_EXPORT_CONTACT.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
