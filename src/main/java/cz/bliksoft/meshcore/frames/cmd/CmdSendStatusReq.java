package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendStatusReq extends CommandsWithSentResponse {

	final byte[] pubkey;

	public CmdSendStatusReq(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_STATUS_REQ;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());

		bb.put(pubkey);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_STATUS_RESPONSE;
	}
}
