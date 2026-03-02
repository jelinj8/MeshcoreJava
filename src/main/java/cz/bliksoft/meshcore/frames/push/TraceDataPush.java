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
	final int pathLen;

	final int flags;

	final byte[] tag;
	final byte[] authCode;

	public class PathRecord {
		byte[] hash;
		int snr;
	}

	final List<PathRecord> path;

	private final byte[] pathHashes;
	private final byte[] pathSnr;

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
