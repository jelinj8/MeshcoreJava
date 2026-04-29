package cz.bliksoft.meshcore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cz.bliksoft.meshcore.frames.Frame;

/**
 * Thread-safe registry that maps frame types to their {@link FrameListener}
 * instances and dispatches incoming frames to all matching listeners.
 *
 * <p>
 * Listeners are stored in {@link CopyOnWriteArrayList}s so that registration
 * and removal are safe while dispatch is in progress.
 * </p>
 */
public final class FrameListenerRegistry {

	private final Map<Class<? extends Frame>, CopyOnWriteArrayList<FrameListener<? super Frame>>> listeners = new ConcurrentHashMap<>();

	/**
	 * Registers a listener for the given frame type.
	 *
	 * @param <T>      the frame type
	 * @param type     the concrete frame class to listen for
	 * @param listener the listener to register
	 */
	@SuppressWarnings("unchecked")
	public <T extends Frame> void register(Class<T> type, FrameListener<? super T> listener) {
		listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add((FrameListener<? super Frame>) listener);
	}

	/**
	 * Removes a previously registered listener for the given frame type.
	 *
	 * @param type     the frame class the listener was registered for
	 * @param listener the listener to remove
	 */
	public void remove(Class<? extends Frame> type, FrameListener<?> listener) {
		List<FrameListener<? super Frame>> list = listeners.get(type);
		if (list != null) {
			list.remove(listener); // remove(Object)
			if (list.isEmpty())
				listeners.remove(type, list);
		}
	}

	/**
	 * Removes a listener from all frame-type registrations.
	 *
	 * <p>
	 * Slower than {@link #remove(Class, FrameListener)} because it scans all type
	 * buckets, but useful when the original type is unknown.
	 * </p>
	 *
	 * @param listener the listener to remove
	 */
	public void removeFrameListener(FrameListener<?> listener) {
		listeners.forEach((type, list) -> {
			list.removeIf(l -> l == listener);
			if (list.isEmpty())
				listeners.remove(type, list);
		});
	}

	/**
	 * Dispatches {@code frame} to all listeners whose registered type is
	 * assignment-compatible with the frame's runtime class.
	 *
	 * @param frame the frame to dispatch
	 */
	public void dispatch(Frame frame) {
		listeners.forEach((cls, list) -> {
			if (cls.isInstance(frame)) {
				for (FrameListener<? super Frame> l : list) {
					l.onFrame(frame);
				}
			}
		});
	}
}
