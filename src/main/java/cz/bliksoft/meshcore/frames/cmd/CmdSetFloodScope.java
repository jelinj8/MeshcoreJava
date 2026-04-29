package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets or clears the active flood scope transport key used
 * to restrict flood-routed packets. Expects a {@code RESP_OK} response.
 */
public class CmdSetFloodScope extends CommandFrame {

	final byte[] scope;

	/**
	 * @param scope 16-byte transport key restricting flood scope, or {@code null}
	 *              to clear (global scope)
	 */
	public CmdSetFloodScope(byte[] scope) {
		this.scope = scope;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_FLOOD_SCOPE;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put((byte) 0);
		if (scope != null)
			bb.put(scope);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
