package cz.bliksoft.meshcore.frames.push;

import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Sent when a known contact re-advertises. Contains only the public key; use
 * {@link cz.bliksoft.meshcore.companion.MeshcoreCompanionConfig#getContact(String)}
 * to look up the cached contact info, or issue CMD_GET_CONTACT_BY_KEY to
 * refresh.
 *
 * @see NewAdvertPush for the first-seen / updated-info variant
 */
public class AdvertPush extends ContactFrameGroup {

	final byte[] pubkey;

	/**
	 * Returns the full public key of the contact that re-advertised.
	 *
	 * @return 32-byte public key
	 */
	public byte[] getPubkey() {
		return pubkey;
	}

	public AdvertPush(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		pubkey = br.readBytes(Settings.PUBKEY_SIZE);
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.PUSH_ADVERT;
	}

	@Override
	public String toString() {
		return String.format("PUSH_ADVERT pubkey=%s", MeshcoreUtils.hex(pubkey));

//		// might not be saved, getting the contact causes ConcurrentModification exception.
//		Contact c = companion.getContact(pubkey);
//		return String.format("PUSH_ADVERT pubkey=%s [%s]", MeshcoreUtils.hex(pubkey),
//				c != null ? c.getName() : "?");
	}
}
