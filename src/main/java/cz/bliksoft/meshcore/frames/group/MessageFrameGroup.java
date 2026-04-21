package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;

public abstract class MessageFrameGroup extends ResponseFrame {

	public MessageFrameGroup(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	private LogRXDataPush pairedLogFrame;

	public LogRXDataPush getPairedLogFrame() {
		return pairedLogFrame;
	}

	public void setPairedLogFrame(LogRXDataPush frame) {
		this.pairedLogFrame = frame;
	}

}
