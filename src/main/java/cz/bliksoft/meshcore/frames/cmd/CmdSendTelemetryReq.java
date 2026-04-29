package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that requests telemetry data from a remote node or from the
 * device itself. Expects a {@link cz.bliksoft.meshcore.frames.resp.Sent}
 * acknowledgement (for remote nodes) or a {@code PUSH_TELEMETRY_RESPONSE} push
 * (for self-telemetry).
 */
public class CmdSendTelemetryReq extends CommandsWithSentResponse {

	final byte[] pubkey;

	/**
	 * @param pubkey 32-byte public key of the target node, or {@code null} to
	 *               request self-telemetry
	 */
	public CmdSendTelemetryReq(byte[] pubkey) {
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

		if (pubkey != null)
			bb.put(pubkey);

		return bb.toArray();
	}

	public static final byte[] EXPECT = new byte[] { ResponseFrameType.PUSH_TELEMETRY_RESPONSE.code(),
			ResponseFrameType.RESP_ERR.code() };

	@Override
	public byte[] expectedResponses() {
		if (pubkey != null)
			return EXPECTED_SENT;
		else
			return EXPECT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		if (pubkey != null)
			return ResponseFrameType.PUSH_TELEMETRY_RESPONSE;
		else
			return null;
	}

}
