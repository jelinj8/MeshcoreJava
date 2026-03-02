package cz.bliksoft.meshcore;

import cz.bliksoft.meshcore.frames.Frame;

public interface FrameListener<T extends Frame> {
	void onFrame(T frame);
}
