package cz.bliksoft.meshcore.frames.cmd;

import java.util.Arrays;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Command frame that sends a status request to a contact identified by public
 * key. Expects a sent acknowledgement followed by a
 * {@link cz.bliksoft.meshcore.frames.push.StatusResponsePush} push response.
 */
public class CmdSendStatusReq extends CommandsWithSentResponse {

	final byte[] pubkey;

	/**
	 * @param pubkey the 32-byte public key of the destination contact
	 */
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

	/**
	 * The firmware's PUSH_STATUS_RESPONSE carries only a 6-byte pubkey prefix (see
	 * {@link cz.bliksoft.meshcore.frames.push.StatusResponsePush#getResponseKey()}),
	 * not the request tag from the {@link cz.bliksoft.meshcore.frames.resp.Sent}
	 * acknowledgement, so the result key must be keyed off the target pubkey
	 * instead of the default tag-based key.
	 */
	@Override
	public String getResultKey(ResponseFrame callResult) {
		this.callResult = (cz.bliksoft.meshcore.frames.resp.Sent) callResult;
		return MeshcoreUtils.hex(Arrays.copyOf(pubkey, 6));
	}
}
