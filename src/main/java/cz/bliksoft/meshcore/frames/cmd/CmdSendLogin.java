package cz.bliksoft.meshcore.frames.cmd;

import java.util.Arrays;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.frames.resp.Sent;
import cz.bliksoft.meshcore.utils.ByteBuilder;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class CmdSendLogin extends CommandsWithSentResponse {

	final byte[] pubkey;
	final String password;

	public CmdSendLogin(byte[] pubkey, String password) {
		this.pubkey = pubkey;
		this.password = password;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_LOGIN;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(pubkey);
		bb.put(password);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_LOGIN_SUCCESS;
	}

	@Override
	public String getResultKey(ResponseFrame callResult) {
		// Save callResult for getExpectedResponseTimeout(), but key off prefix6
		// (first 6 bytes of destination pubkey) to match
		// LoginSuccessPush.getResponseKey()
		this.callResult = (Sent) callResult;
		return MeshcoreUtils.hex(Arrays.copyOf(pubkey, 6));
	}
}
