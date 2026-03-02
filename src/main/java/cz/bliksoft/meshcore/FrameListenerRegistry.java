package cz.bliksoft.meshcore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cz.bliksoft.meshcore.frames.Frame;

public final class FrameListenerRegistry {

	private final Map<Class<? extends Frame>, CopyOnWriteArrayList<FrameListener<? super Frame>>> listeners = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T extends Frame> void register(Class<T> type, FrameListener<? super T> listener) {
		listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add((FrameListener<? super Frame>) listener);
	}

	public void remove(Class<? extends Frame> type, FrameListener<?> listener) {
		List<FrameListener<? super Frame>> list = listeners.get(type);
		if (list != null) {
			list.remove(listener); // remove(Object)
			if (list.isEmpty())
				listeners.remove(type, list);
		}
	}

	/** more expensive but universal */
	public void removeFrameListener(FrameListener<?> listener) {
		listeners.forEach((type, list) -> {
			list.removeIf(l -> l == listener);
			if (list.isEmpty())
				listeners.remove(type, list);
		});
	}

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
