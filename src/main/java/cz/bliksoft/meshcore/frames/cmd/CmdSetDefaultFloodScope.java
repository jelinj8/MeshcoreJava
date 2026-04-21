package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetDefaultFloodScope extends CommandFrame {

	/** Fixed-width field for scope name in the wire format. */
	private static final int SCOPE_NAME_LEN = 31;
	/** Fixed-width field for scope key in the wire format. */
	private static final int SCOPE_KEY_LEN = 16;

	final String scopeName;
	final byte[] scopeKey;

	/**
	 * @param scopeName name of the default flood scope, or null to clear
	 * @param scopeKey  16-byte transport key, or null to clear
	 */
	public CmdSetDefaultFloodScope(String scopeName, byte[] scopeKey) {
		this.scopeName = scopeName;
		this.scopeKey = scopeKey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_DEFAULT_FLOOD_SCOPE;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		if (scopeName != null && scopeKey != null) {
			bb.put(scopeName, SCOPE_NAME_LEN);
			bb.put(scopeKey);
		}
		// length 1 (command byte only) signals clear
		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
