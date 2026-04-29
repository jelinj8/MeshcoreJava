package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that initiates a path discovery request to find a route to a
 * remote node. Expects a {@link cz.bliksoft.meshcore.frames.resp.Sent}
 * acknowledgement followed by a {@code PUSH_PATH_DISCOVERY_RESPONSE} push.
 */
public class CmdSendPathDiscoveryReq extends CommandsWithSentResponse {

	final byte[] pubkey;

	/**
	 * @param pubkey 32-byte public key of the target node to discover a path to
	 */
	public CmdSendPathDiscoveryReq(byte[] pubkey) {
		this.pubkey = pubkey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_PATH_DISCOVERY_REQ;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put((byte) 0x00);

		bb.put(pubkey);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_PATH_DISCOVERY_RESPONSE;
	}
}
