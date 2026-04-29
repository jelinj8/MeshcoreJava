package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that queries device information and negotiates the protocol
 * version. Expects a {@link cz.bliksoft.meshcore.frames.resp.DeviceInfo}
 * response.
 */
public class CmdDeviceQuery extends CommandFrame {

	@Override
	public byte[] getBytes() {
		// type
		// supported protocol version
		return new byte[] { getFrameType().code(), Settings.IMPLEMENTED_PROTOCOL_VERSION };
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_DEVICE_QEURY;
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_DEVICE_INFO.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
