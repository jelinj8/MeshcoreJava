package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sets or clears the active flood scope transport key used
 * to restrict flood-routed packets, or forces sending completely unscoped
 * (protocol v12+). Expects a {@code RESP_OK} response.
 */
public class CmdSetFloodScope extends CommandFrame {

	final byte[] scope;
	final boolean unscoped;

	/**
	 * @param scope 16-byte transport key restricting flood scope, or {@code null}
	 *              to clear the override (revert to the persisted default scope,
	 *              if any)
	 */
	public CmdSetFloodScope(byte[] scope) {
		this.scope = scope;
		this.unscoped = false;
	}

	private CmdSetFloodScope(boolean unscoped) {
		this.scope = null;
		this.unscoped = unscoped;
	}

	/**
	 * Forces flood-routed packets to be sent completely unscoped (protocol
	 * v12+), overriding even the persisted default scope. This is distinct from
	 * {@code new CmdSetFloodScope(null)}, which merely clears the override and
	 * falls back to the persisted default scope.
	 */
	public static CmdSetFloodScope unscoped() {
		return new CmdSetFloodScope(true);
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_FLOOD_SCOPE;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		if (unscoped) {
			bb.put((byte) 1);
		} else {
			bb.put((byte) 0);
			if (scope != null)
				bb.put(scope);
		}

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
