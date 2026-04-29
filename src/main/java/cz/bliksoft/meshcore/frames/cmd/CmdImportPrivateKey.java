package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that imports a private key into the device. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Disabled} response.
 */
public class CmdImportPrivateKey extends CommandFrame {

	final byte[] privateKey;

	/**
	 * @param privateKey the raw private key bytes to import
	 */
	public CmdImportPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_IMPORT_PRIVATE_KEY;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(privateKey);

		return bb.toArray();
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_OK.code(),
			ResponseFrameType.RESP_DISABLED.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
