package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class EndOfContacts extends ContactFrameGroup {

	final long lastUpdated;

	public long getLastUpdated() {
		return lastUpdated;
	}

	public EndOfContacts(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		lastUpdated = br.readUInt32LE();
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_END_OF_CONTACTS;
	}

	@Override
	public String toString() {
		if (lastUpdated > 0)
			return String.format("RESP_END_OF_CONTACTS, lastUpdated=%s", MeshcoreUtils.formatMeshcoreTs(lastUpdated));
		else
			return "RESP_END_OF_CONTACTS, lastUpdated=?";
	}
}
