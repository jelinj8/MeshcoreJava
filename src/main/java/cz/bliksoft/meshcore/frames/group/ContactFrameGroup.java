package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;

/**
 * Marker base class for response frames that carry contact information.
 *
 * <p>
 * Registering a {@link cz.bliksoft.meshcore.FrameListener} for this type allows
 * an application to receive all contact-related response frames
 * ({@link cz.bliksoft.meshcore.frames.resp.Contact},
 * {@link cz.bliksoft.meshcore.frames.push.ContactsFullPush}, etc.) through a
 * single listener.
 * </p>
 */
public abstract class ContactFrameGroup extends ResponseFrame {

	public ContactFrameGroup(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

}
