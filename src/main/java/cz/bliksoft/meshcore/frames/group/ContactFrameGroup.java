package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;

public abstract class ContactFrameGroup extends ResponseFrame {

	public ContactFrameGroup(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

}
