package cz.bliksoft.meshcore.frames;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public abstract class ResponseFrame extends Frame {
	protected byte[] data;
	protected final MeshcoreCompanion companion;

	/**
	 * Returns the companion instance that received this frame.
	 *
	 * @return the originating companion
	 */
	public MeshcoreCompanion getCompanion() {
		return companion;
	}

	/**
	 * Raw frame bytes as received from the device.
	 *
	 * @return raw frame bytes
	 */
	public byte[] getData() {
		return data;
	}

	public ResponseFrame(MeshcoreCompanion source, byte[] data) {
		this.data = data;
		this.companion = source;
		this.receivedAt = System.currentTimeMillis();
	}

	/**
	 * Returns the frame type code that identifies this response.
	 *
	 * @return response frame type
	 */
	public abstract ResponseFrameType getFrameType();

	/**
	 * Returns {@code true} if this frame's type equals {@code type}.
	 *
	 * @param type the frame type to compare against
	 * @return true if frame types match
	 */
	public boolean is(ResponseFrameType type) {
		return getFrameType().equals(type);
	}

	@Override
	public boolean is(FrameType type) {
		return FrameType.RESPONSE.equals(type);
	}

	@Override
	public byte getTypeCode() {
		return getFrameType().code();
	}

	@Override
	public byte[] getBytes() {
		return data;
	}

	@Override
	public String toString() {
		return String.format("%s UNPARSED: %s", getFrameType(), MeshcoreUtils.hex(data));
	}

	/**
	 * Returns the {@link System#currentTimeMillis()} timestamp when this frame was
	 * received.
	 *
	 * @return wall-clock millisecond timestamp
	 */
	public long getReceivedAt() {
		return receivedAt;
	}

	private final long receivedAt;

	public String getResponseKey() {
		return null;
	}

	/**
	 * Compose a cache key from a response type and an identifying value.
	 *
	 * @param type  response frame type
	 * @param value identifying value (e.g. hex prefix6 or tag)
	 * @return composite key string used in the response-waiter map
	 */
	public static String getFrameKey(ResponseFrameType type, String value) {
		return String.format("%s:%s", MeshcoreUtils.hex(type.code()), value);
	}

	/**
	 * Returns the composite cache key used to pair this response with a pending
	 * waiter (e.g. composed from the frame type and prefix6/tag). Returns
	 * {@code null} by default; override in frames that support async matching.
	 *
	 * @return composite cache key, or {@code null} if this frame has no async match
	 */
	public String getFrameKey() {
		String responseKey = getResponseKey();
		if (responseKey != null)
			return getFrameKey(getFrameType(), responseKey);
		return null;
	}
}
