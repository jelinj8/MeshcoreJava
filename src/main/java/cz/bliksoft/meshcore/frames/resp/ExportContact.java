package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

public class ExportContact extends ResponseFrame {

	/**
	 * Raw serialized advert Packet blob. Can be passed directly to
	 * {@link cz.bliksoft.meshcore.frames.cmd.CmdImportContact}.
	 */
	final byte[] advertPacket;

	public byte[] getAdvertPacket() {
		return advertPacket;
	}

	public ExportContact(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip(); // response type byte
		advertPacket = br.readBytes();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_EXPORT_CONTACT;
	}

	@Override
	public String toString() {
		return String.format("RESP_EXPORT_CONTACT advertPacket(%d bytes)", advertPacket.length);
	}

}
