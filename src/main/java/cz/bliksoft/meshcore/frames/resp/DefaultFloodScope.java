package cz.bliksoft.meshcore.frames.resp;

import java.util.Arrays;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class DefaultFloodScope extends ResponseFrame {

	private static final int SCOPE_NAME_LEN = 31;
	private static final int SCOPE_KEY_LEN = 16;
	private static final int FULL_FRAME_LEN = 1 + SCOPE_NAME_LEN + SCOPE_KEY_LEN;

	/** Scope name, or null if no default scope is set. */
	public String getScopeName() {
		return scopeName;
	}

	/** 16-byte transport key, or null if no default scope is set. */
	public byte[] getScopeKey() {
		return scopeKey != null ? Arrays.copyOf(scopeKey, scopeKey.length) : null;
	}

	public boolean hasScope() {
		return scopeName != null;
	}

	final String scopeName;
	final byte[] scopeKey;

	public DefaultFloodScope(MeshcoreCompanion source, byte[] raw) {
		super(source, raw);
		if (raw.length >= FULL_FRAME_LEN) {
			ByteReader br = new ByteReader(raw);
			br.skip(); // code byte
			scopeName = br.readFixedCString(SCOPE_NAME_LEN);
			scopeKey = br.readBytes(SCOPE_KEY_LEN);
		} else {
			scopeName = null;
			scopeKey = null;
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_DEFAULT_FLOOD_SCOPE;
	}
}
