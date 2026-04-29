package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;

/**
 * Push frame sent by the device to notify that an incoming message is waiting
 * to be retrieved; the message details are carried in the inherited
 * {@link MessageFrameGroup} fields.
 */
public class MessageWaitingPush extends MessageFrameGroup {

	public MessageWaitingPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_MSG_WAITING;
	}

	@Override
	public String toString() {
		return "PUSH_MESSAGE_WAITING";
	}

}
