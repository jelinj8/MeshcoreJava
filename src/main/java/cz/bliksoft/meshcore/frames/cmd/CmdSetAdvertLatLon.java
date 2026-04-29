package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetAdvertLatLon extends CommandFrame {

	final double lat;
	final double lon;
	final Integer alt;

	/**
	 * Set the device's advertised GPS coordinates.
	 *
	 * @param lat latitude in decimal degrees
	 * @param lon longitude in decimal degrees
	 * @param alt altitude in metres, or {@code null} to omit (not supported by all
	 *            firmware versions)
	 */
	public CmdSetAdvertLatLon(double lat, double lon, Integer alt) {
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_ADVERT_LATLON;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.putInt32LE((int) Math.round(lat * 1000000));
		bb.putInt32LE((int) Math.round(lon * 1000000));
		if (alt != null)
			bb.putInt32LE(alt);

		return bb.toArray();
	}

}
