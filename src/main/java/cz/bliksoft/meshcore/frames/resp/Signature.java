package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdSignFinish} carrying
 * the 64-byte Ed25519 signature produced by the device over the data submitted
 * via {@link cz.bliksoft.meshcore.frames.cmd.CmdSignData}.
 */
public class Signature extends ResponseFrame {

	final byte[] signature;

	/**
	 * @return the 64-byte Ed25519 signature computed by the device
	 */
	public byte[] getSignature() {
		return signature;
	}

	public Signature(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		signature = br.readBytes(64);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_SIGNATURE;
	}

}
