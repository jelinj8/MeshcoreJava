package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;

public class NoMoreMessages extends MessageFrameGroup {

	public NoMoreMessages(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_NO_MORE_MESSAGES;
	}

	@Override
	public String toString() {
		return "RESP_NO_MORE_MESSAGES";
	}
}
