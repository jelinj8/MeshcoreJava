package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdImportContact extends CommandFrame {

	final byte[] contactData;

	public CmdImportContact(byte[] data) {
		this.contactData = data;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_IMPORT_CONTACT;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(contactData);
		return bb.toArray();
	}

}
