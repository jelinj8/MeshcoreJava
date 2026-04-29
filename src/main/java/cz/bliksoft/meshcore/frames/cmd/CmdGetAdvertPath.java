package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that requests the advertisement path to a contact identified by
 * public key. Expects a {@link cz.bliksoft.meshcore.frames.resp.AdvertPath}
 * response, or {@link cz.bliksoft.meshcore.frames.resp.Error} on failure.
 */
public class CmdGetAdvertPath extends CommandFrame {

	final byte[] pubkey;
	final byte reserved;

	/**
	 * @param pubkey the 32-byte public key of the contact whose advert path is
	 *               requested
	 */
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
