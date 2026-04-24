package cz.bliksoft.meshcore.companion;

import cz.bliksoft.meshcore.frames.resp.Contact;

/**
 * Receives notifications whenever the local contacts map changes. Callbacks are
 * dispatched on the single-threaded {@code eventExecutor}; do not perform
 * blocking I/O or call blocking companion methods inside them.
 */
public interface ContactListener {

	/**
	 * A contact was added to the saved contacts map.
	 * <p>
	 * Fires for every {@code RESP_CONTACT} whose key is new (initial or incremental
	 * sync). Discovered-but-unsaved contacts ({@code PUSH_NEW_ADVERT}) go to the
	 * archive and do NOT trigger this callback.
	 */
	void onContactAdded(Contact contact);

	/**
	 * An existing contact in the local map was updated.
	 * <p>
	 * Fires when a {@code RESP_CONTACT} overwrites an existing entry (incremental
	 * sync update or refetch after {@code PUSH_ADVERT} /
	 * {@code PUSH_PATH_UPDATED}).
	 */
	void onContactUpdated(Contact contact);

	/**
	 * A saved contact was removed from the local map (moved to archive).
	 * <p>
	 * Fires on {@code PUSH_CONTACT_DELETED}.
	 */
	void onContactRemoved(Contact contact);
}
