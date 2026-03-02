package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;

public abstract class MessageFrameGroup extends ResponseFrame {

	public MessageFrameGroup(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

}
