package cz.bliksoft.meshcore.frames;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public abstract class ResponseFrame extends Frame {
	protected byte[] data;
	protected final MeshcoreCompanion companion;

	public byte[] getData() {
		return data;
	}

	public ResponseFrame(MeshcoreCompanion source, byte[] data) {
		this.data = data;
		this.companion = source;
		this.receivedAt = System.currentTimeMillis();
	}

	public abstract ResponseFrameType getFrameType();

	public boolean is(ResponseFrameType type) {
		return getFrameType().equals(type);
	}

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

	public long getReceivedAt() {
		return receivedAt;
	}

	private final long receivedAt;

	public String getResponseKey() {
		return null;
	}

	/**
	 * key composer for identified frame cache
	 * 
	 * @param type
	 * @param value
	 * @return
	 */
	public static String getFrameKey(ResponseFrameType type, String value) {
		return String.format("%s:%s", MeshcoreUtils.hex(type.code()), value);
	}

	/**
	 * Value identifying the response frame for pairing with sent request. E.g.
	 * prefix6, pubkey or tag. <code>null</code> by default.
	 * 
	 * @return
	 */
	public String getFrameKey() {
		String responseKey = getResponseKey();
		if (responseKey != null)
			return getFrameKey(getFrameType(), responseKey);
		return null;
	}
}
