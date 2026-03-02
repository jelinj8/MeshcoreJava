package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendSelfAdvert extends CommandFrame {

	public enum AdvertMethod {
		FLOOD, SINGLE
	}

	final AdvertMethod method;

	public CmdSendSelfAdvert(AdvertMethod method) {
		this.method = method;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_SELF_ADVERT;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		if (method != null)
			bb.put((byte) (method == AdvertMethod.FLOOD ? 0x01 : 0x00));
		return bb.toArray();
	}

}
