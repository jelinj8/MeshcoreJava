package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdImportContact extends CommandFrame {

	/**
	 * Raw serialized advert Packet blob, as produced by the firmware's
	 * {@code Packet::writeTo()} and returned by {@code CMD_EXPORT_CONTACT}.
	 * Must be a {@code PAYLOAD_TYPE_ADVERT} packet; minimum size is 98 bytes.
	 */
	final byte[] advertPacket;

	public CmdImportContact(byte[] advertPacket) {
		this.advertPacket = advertPacket;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_IMPORT_CONTACT;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(advertPacket);
		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK_ERR;
	}

}
