package cz.bliksoft.meshcore.frames.push;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Push frame received when a trace-path response arrives, carrying per-hop SNR
 * readings, an auth code, and a tag that matches the originating
 * CMD_SEND_TRACE_PATH request.
 */
public class TraceDataPush extends ResponseFrame {

	/**
	 * @return reserved byte from the wire frame (currently unused)
	 */
	public byte getReserved() {
		return reserved;
	}

	/**
	 * @return raw encoded path length (number of bytes covering all hop hashes);
	 *         divide by hash size to get hop count
	 */
	public int getPathLen() {
		return pathLen;
	}

	/**
	 * @return flags bitmask; bits [1:0] encode the hash size per hop (path_sz)
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * @return 4-byte tag linking this push to the originating CMD_SEND_TRACE_PATH /
	 *         RESP_SENT
	 */
	public byte[] getTag() {
		return tag;
	}

	/**
	 * @return 4-byte auth code sent with the trace packet to verify path
	 *         authenticity
	 */
	public byte[] getAuthCode() {
		return authCode;
	}

	/**
	 * @return ordered list of per-hop path records, each containing a node hash and
	 *         SNR reading
	 */
	public List<PathRecord> getPath() {
		return path;
	}

	/**
	 * @return SNR to this (receiving) node as a scaled integer; divide by 4.0 to
	 *         get dB
	 */
	public int getFinalSnr4() {
		return finalSnr4;
	}

	final byte reserved;
	/** Number of hops in the trace path (= number of path-hash entries). */
	final int pathLen;

	/**
	 * Flags bitmask: bits [1:0] = path_sz – hash size per hop: 0 = 1 byte/hop 1 = 2
	 * bytes/hop 2 = 4 bytes/hop (path_sz introduced in firmware v1.11; older
	 * firmware always uses 0)
	 */
	final int flags;

	/**
	 * Tag used to match this push to the originating CMD_SEND_TRACE_PATH /
	 * RESP_SENT.
	 */
	final byte[] tag;
	/** Auth code sent with the trace packet, used to verify path authenticity. */
	final byte[] authCode;

	public class PathRecord {
		/** Per-hop node hash; length = 1 << path_sz bytes. */
		byte[] hash;
		/** Signed SNR4 value at this hop (int8_t); dB = snr4 / 4.0. */
		int snr4;
	}

	final List<PathRecord> path;

	/** Signed int8; SNR to THIS (receiving) node in dB = finalSnr4 / 4.0. */
	final int finalSnr4;

	public TraceDataPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		reserved = br.readByte();
		pathLen = br.readUnsignedByte();

		flags = br.readUnsignedByte();
		int pathSize = flags & 0x03;
		int hashBytes = 1 << pathSize;
		int hopCount = pathLen >> pathSize;

		tag = br.readBytes(4);
		authCode = br.readBytes(4);

		byte[] pathHashes = br.readBytes(pathLen);
		byte[] pathSnr = br.readBytes(hopCount);

		path = new ArrayList<>(hopCount);
		for (int i = 0; i < hopCount; i++) {
			PathRecord r = new PathRecord();
			r.hash = new byte[hashBytes];
			System.arraycopy(pathHashes, i * hashBytes, r.hash, 0, hashBytes);
			r.snr4 = pathSnr[i]; // signed int8_t
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
		String pathString = path.stream()
				.map(r -> String.format("%s(snr:%.2f)", MeshcoreUtils.hex(r.hash), r.snr4 / 4.0))
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
