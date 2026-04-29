package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that queries whether the device has an established connection
 * with the specified contact. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdHasConnection extends CommandFrame {

	final byte[] pubkey;

	/**
	 * @param pubkey public key of the contact to check for an active connection
	 */
	public CmdHasConnection(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_HAS_CONNECTION;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(pubkey);

		return bb.toArray();
	}

}
