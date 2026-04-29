package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends a binary request to a contact identified by public
 * key. Expects a sent acknowledgement followed by a
 * {@link cz.bliksoft.meshcore.frames.push.BinaryResponsePush} push response.
 */
public class CmdSendBinaryReq extends CommandsWithSentResponse {

	final byte[] pubkey32;
	final byte[] data;

	/**
	 * @param pubkey32 the 32-byte public key of the destination contact
	 * @param data     the binary payload to send
	 */
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
