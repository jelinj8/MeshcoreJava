package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetRadioParams extends CommandFrame {

	final long freq;
	final long bw;
	final byte sf;
	final byte cr;
	final Boolean repeat;

	/**
	 * 
	 * @param freq (Hz)
	 * @param bw (Hz)
	 * @param sf
	 * @param cr
	 * @param repeat
	 */
	public CmdSetRadioParams(long freq, long bw, byte sf, byte cr, Boolean repeat) {
		this.freq = freq;
		this.bw = bw;
		this.sf = sf;
		this.cr = cr;
		this.repeat = repeat;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_RADIO_PARAMS;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.putUInt32LE(freq / 1000); // firmware expects kHz
		bb.putUInt32LE(bw);          // firmware expects Hz
		bb.put(sf);
		bb.put(cr);

		// v9+
		if (repeat != null)
			bb.put((byte) (Boolean.TRUE.equals(repeat) ? 0x01 : 0x00));

		return bb.toArray();
	}

}
