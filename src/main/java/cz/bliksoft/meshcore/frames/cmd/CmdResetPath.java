package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that clears the cached outbound path to a contact, forcing the
 * next send to use flood routing. Does not expect a specific response frame.
 */
public class CmdResetPath extends CommandFrame {

	final byte[] pubkey;

	/**
	 * Clear the cached outbound path to a contact, forcing the next send to use
	 * flood routing.
	 *
	 * @param pubkey full 32-byte public key of the contact
	 */
	public CmdResetPath(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_RESET_PATH;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(pubkey);
		return bb.toArray();
	}

}
