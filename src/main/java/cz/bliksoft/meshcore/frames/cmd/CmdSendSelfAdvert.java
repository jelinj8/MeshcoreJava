package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that sends the device's own advertisement packet to the mesh
 * network. Expects a {@link cz.bliksoft.meshcore.frames.resp.Ok} or
 * {@link cz.bliksoft.meshcore.frames.resp.Error} response.
 */
public class CmdSendSelfAdvert extends CommandFrame {

	public enum AdvertMethod {
		FLOOD, SINGLE
	}

	final AdvertMethod method;

	/**
	 * @param method the advertisement propagation method
	 *               ({@link AdvertMethod#FLOOD} or {@link AdvertMethod#SINGLE})
	 */
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
