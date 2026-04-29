package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that requests the export of the device's private key. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.PrivateKey} response, or
 * {@link cz.bliksoft.meshcore.frames.resp.Disabled} if the feature is disabled.
 */
public class CmdExportPrivateKey extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_EXPORT_PRIVATE_KEY;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { CommandFrameType.CMD_EXPORT_PRIVATE_KEY.code() };
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_PRIVATE_KEY.code(),
			ResponseFrameType.RESP_DISABLED.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}
}
