package cz.bliksoft.meshcore.frames.push;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class TraceDataPush extends ResponseFrame {

	public byte getReserved() {
		return reserved;
	}

	public int getPathLen() {
		return pathLen;
	}

	public int getFlags() {
		return flags;
	}

	public byte[] getTag() {
		return tag;
	}

	public byte[] getAuthCode() {
		return authCode;
	}

	public List<PathRecord> getPath() {
		return path;
	}

	public int getFinalSnr4() {
		return finalSnr4;
	}

	final byte reserved;
	/** Number of hops in the trace path (= number of path-hash entries). */
	final int pathLen;

	/**
	 * Flags bitmask:
	 * bits [1:0] = path_sz – hash size per hop:
	 *   0 = 1 byte/hop
	 *   1 = 2 bytes/hop
	 *   2 = 4 bytes/hop
	 * (path_sz introduced in firmware v1.11; older firmware always uses 0)
	 */
	final int flags;

	/** Tag used to match this push to the originating CMD_SEND_TRACE_PATH / RESP_SENT. */
	final byte[] tag;
	/** Auth code sent with the trace packet, used to verify path authenticity. */
	final byte[] authCode;

	public class PathRecord {
		/** Per-hop node hash; length = 1 << path_sz bytes. */
		byte[] hash;
		/** Raw unsigned SNR value at this hop (scale: value / 4.0 = dB). */
		int snr;
	}

	final List<PathRecord> path;

	private final byte[] pathHashes;
	private final byte[] pathSnr;

	/** Signed int8; SNR to THIS (receiving) node in dB = finalSnr4 / 4.0. */
	final int finalSnr4;

	public TraceDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		pathLen = br.readUnsignedByte();
		flags = br.readUnsignedByte();

		int path_size = flags & 0x03;

		tag = br.readBytes(4);
		authCode = br.readBytes(4);

		pathHashes = br.readBytes(pathLen);
		pathSnr = br.readBytes(pathLen >> path_size);

		path = new ArrayList<>(pathLen);
		for (int i = 0; i < pathLen; i++) {
			PathRecord r = new PathRecord();
			r.hash = new byte[path_size];
			System.arraycopy(pathHashes, i * path_size, r.hash, 0, path_size);
			r.snr = pathSnr[i] & 0xFF;
			path.add(r);
		}

		finalSnr4 = br.readSignedByte();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_TRACE_DATA;
	}

	@Override
	public String toString() {
		String pathString = path.stream().map(r -> String.format("%s (%d)", MeshcoreUtils.hex(r.hash), r.snr))
				.collect(Collectors.joining("->"));

		return String.format("PUSH_TRACE_DATA reserved=%d pathLen=%d flags=%d tag=%s authCode=%s path=%s finalSnr=%.2f",
				reserved, pathLen, flags, MeshcoreUtils.hex(tag), MeshcoreUtils.hex(authCode), pathString,
				finalSnr4 / 4.0);
	}

	@Override
	public String getResponseKey() {
		return MeshcoreUtils.hex(tag);
	}
}
