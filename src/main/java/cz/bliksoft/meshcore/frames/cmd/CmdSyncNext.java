package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Command frame that requests the next queued message from the device. Expects
 * a {@link cz.bliksoft.meshcore.frames.resp.NoMoreMessages},
 * {@link cz.bliksoft.meshcore.frames.resp.ChannelMsgRecv}, or
 * {@link cz.bliksoft.meshcore.frames.resp.ContactMsgRecv} response.
 */
public class CmdSyncNext extends CommandFrame {

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SYNC_NEXT_MESSAGE;
	}

	@Override
	public byte[] getBytes() {
		return new byte[] { getFrameType().code() };
	}

	@Override
	public byte[] expectedResponses() {
		return new byte[] { ResponseFrameType.RESP_NO_MORE_MESSAGES.code(),
				ResponseFrameType.RESP_CHANNEL_MSG_RECV.code(), ResponseFrameType.RESP_CHANNEL_MSG_RECV_V3.code(),
				ResponseFrameType.RESP_CONTACT_MSG_RECV.code(), ResponseFrameType.RESP_CONTACT_MSG_RECV_V3.code() };
	}

}
