package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AutoAddConfigFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Autoadd contacts config, use {@link AutoAddConfigFlags}
 */
public class AutoaddConfig extends ResponseFrame {

	final byte autoaddConfig;

	public byte getAutoaddConfig() {
		return autoaddConfig;
	}

	public AutoaddConfig(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		autoaddConfig = data[1];
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_AUTOADD_CONFIG;
	}

}
