package cz.bliksoft.meshcore.frames.cmd;

import java.time.Instant;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetDeviceTime extends CommandFrame {

	final long timestamp;

	/**
	 * 
	 * @param timestamp default now, ILLEGAL_ARG if sooner than device's current
	 *                  time
	 */
	public CmdSetDeviceTime(Long timestamp) {
		this.timestamp = timestamp == null ? Instant.now().getEpochSecond() : timestamp;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_DEVICE_TIME;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.putUInt32LE(timestamp);
		return bb.toArray();
	}

}
