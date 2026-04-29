package cz.bliksoft.meshcore;

import cz.bliksoft.meshcore.frames.Frame;

/**
 * Callback interface for receiving typed frames dispatched by a
 * {@link cz.bliksoft.meshcore.companion.MeshcoreCompanion MeshcoreCompanion}.
 *
 * @param <T> the concrete {@link Frame} type this listener handles
 */
public interface FrameListener<T extends Frame> {

	/**
	 * Invoked when a frame of type {@code T} is received from the device.
	 *
	 * @param frame the received frame
	 */
	void onFrame(T frame);
}
