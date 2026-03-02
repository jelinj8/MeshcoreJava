package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendBinaryReq extends CommandsWithSentResponse {

	final byte[] pubkey32;
	final byte[] data;

	public CmdSendBinaryReq(byte[] pubkey32, byte[] data) {
		this.pubkey32 = pubkey32;
		this.data = data;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_BINARY_REQ;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());

		bb.put(pubkey32);
		bb.put(data);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_BINARY_RESPONSE;
	}
}
