package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendTracePath extends CommandsWithSentResponse {

	final byte[] tag;
	final long auth;
	final byte flags;
	final byte[] path;

	/**
	 * 
	 * @param tag
	 * @param auth
	 * @param payload
	 * @param flags   2LSB = path size (1-3)
	 * @param path
	 */
	public CmdSendTracePath(byte[] tag, long auth, byte flags, byte[] path) {
		this.tag = tag;
		this.auth = auth;
		this.flags = flags;
		this.path = path;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_TRACE_PATH;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(tag, 4);
		bb.putUInt32LE(auth);
		bb.put(flags);
		bb.put(path);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_TRACE_DATA;
	}
	
}
