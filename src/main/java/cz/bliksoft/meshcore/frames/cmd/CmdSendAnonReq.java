package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends an anonymous message to a node identified by
 * public key. Expects a sent acknowledgement; no push response is tracked for
 * anonymous sends.
 *
 * <p>
 * As of protocol v13 (firmware v1.16.0+), the target pubkey no longer needs to
 * be an existing contact — the device auto-creates a transient contact for it
 * (evicting the oldest one if the table is full, or failing with
 * {@code ERR_CODE_TABLE_FULL}). Transient contacts may later be reported as
 * removed via {@link cz.bliksoft.meshcore.frames.push.ContactDeletedPush} or
 * trigger {@link cz.bliksoft.meshcore.frames.push.ContactsFullPush}.
 * </p>
 */
public class CmdSendAnonReq extends CommandsWithSentResponse {

	final byte[] pubkey;
	final byte[] msgData;

	/**
	 * @param pubkey  the 32-byte public key of the destination contact
	 * @param msgData the message payload to send anonymously
	 */
	public CmdSendAnonReq(byte[] pubkey, byte[] msgData) {
		this.pubkey = pubkey;
		this.msgData = msgData;

	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_ANON_REQ;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());

		bb.put(pubkey);
		bb.put(msgData);

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	/**
	 * can't expect response when sending anonymously? FIXME
	 */
	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return null;
//		return ResponseFrameType.PUSH_SEND_CONFIRMED;
	}
}
