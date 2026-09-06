package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaRouteType;
import cz.bliksoft.meshcore.utils.ByteBuilder;

/**
 * Command frame that injects a fully pre-built raw mesh packet (protocol v13+).
 * This is a low-level escape hatch mirroring the on-air {@code Packet} wire
 * format directly; most callers should use the higher-level send methods
 * instead.
 */
public class CmdSendRawPacket extends CommandFrame {

	/** Maximum path length in path-hash entries (1-byte hashes only). */
	public static final int MAX_PATH_SIZE = 64;

	/** Maximum raw payload size in bytes. */
	public static final int MAX_PACKET_PAYLOAD = 184;

	final byte priority;
	final OtaRouteType routeType;
	final OtaPayloadType payloadType;
	final int[] transportCodes;
	final byte[] path;
	final byte[] payload;

	/**
	 * @param priority       send priority; lower values are sent first (0 = highest
	 *                       priority)
	 * @param routeType      packet route type
	 * @param payloadType    packet payload type
	 * @param transportCodes 2-element array of unsigned 16-bit transport codes;
	 *                       required iff {@code routeType.hasTransportCodes()},
	 *                       must be {@code null} otherwise
	 * @param path           path hash bytes (1 byte per hop), max
	 *                       {@value #MAX_PATH_SIZE} bytes
	 * @param payload        raw payload bytes, max {@value #MAX_PACKET_PAYLOAD}
	 *                       bytes
	 */
	public CmdSendRawPacket(byte priority, OtaRouteType routeType, OtaPayloadType payloadType, int[] transportCodes,
			byte[] path, byte[] payload) {
		if (path.length > MAX_PATH_SIZE)
			throw new IllegalArgumentException("path too long: " + path.length + " > " + MAX_PATH_SIZE);
		if (payload.length > MAX_PACKET_PAYLOAD)
			throw new IllegalArgumentException("payload too long: " + payload.length + " > " + MAX_PACKET_PAYLOAD);
		boolean needsTransportCodes = routeType.hasTransportCodes();
		if (needsTransportCodes && (transportCodes == null || transportCodes.length != 2))
			throw new IllegalArgumentException("transportCodes (2 elements) required for route type " + routeType);
		if (!needsTransportCodes && transportCodes != null)
			throw new IllegalArgumentException("transportCodes not allowed for route type " + routeType);

		this.priority = priority;
		this.routeType = routeType;
		this.payloadType = payloadType;
		this.transportCodes = transportCodes;
		this.path = path;
		this.payload = payload;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_RAW_PACKET;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put(priority);

		int header = (routeType.code() & 0x03) | ((payloadType.code() & 0x0F) << 2);
		bb.put((byte) header);

		if (transportCodes != null) {
			bb.putUInt16LE(transportCodes[0]);
			bb.putUInt16LE(transportCodes[1]);
		}

		bb.put((byte) path.length);
		bb.put(path);
		bb.put(payload);

		return bb.toArray();
	}
}
