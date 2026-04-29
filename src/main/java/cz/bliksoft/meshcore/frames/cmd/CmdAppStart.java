package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that notifies the device that the application has started and
 * provides the app name. Expects a
 * {@link cz.bliksoft.meshcore.frames.resp.SelfInfo} response.
 */
public class CmdAppStart extends CommandFrame {

	final private String appName;

	/**
	 * @param appName the application name sent to the device (truncated to 40
	 *                characters)
	 */
	public CmdAppStart(String appName) {
		this.appName = appName;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		// type
		bb.put(getFrameType().code());
		// [1..7] reserved
		bb.put(new byte[7]);
		// app name
		bb.put(appName, 40);

		return bb.toArray();
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_APP_START;
	}

	public static final byte[] EXPECTED = new byte[] { ResponseFrameType.RESP_SELF_INFO.code() };

	@Override
	public byte[] expectedResponses() {
		return EXPECTED;
	}

}
