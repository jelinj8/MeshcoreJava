package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdGetAdvertPath extends CommandFrame {

	final byte[] pubkey;
	final byte reserved;

	public CmdGetAdvertPath(byte[] pubkey) {
		this.pubkey = pubkey;
		reserved = 0;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_GET_ADVERT_PATH;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(reserved);
		bb.put(pubkey);
		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_ADVERT_PATH.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
