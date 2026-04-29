package cz.bliksoft.meshcore;

import cz.bliksoft.meshcore.utils.MeshcoreUtils;

/**
 * Protocol-level constants and default timeout/delay values used across the
 * Meshcore library.
 */
public class Settings {

	/** Base send timeout in milliseconds before flood/hop factors are applied. */
	public static final int SEND_TIMEOUT_BASE_MILLIS = 500;

	/**
	 * Multiplier applied to {@link #SEND_TIMEOUT_BASE_MILLIS} for flood-routed
	 * messages.
	 */
	public static final float FLOOD_SEND_TIMEOUT_FACTOR = 16.0f;

	/**
	 * Per-hop timeout multiplier for direct-routed messages (milliseconds per hop).
	 */
	public static final float DIRECT_SEND_PERHOP_FACTOR = 6.0f;

	/** Additional fixed overhead (ms) added to the direct-send timeout per hop. */
	public static final int DIRECT_SEND_PERHOP_EXTRA_MILLIS = 250;

	/** Delay (ms) before flushing a lazy contact-list write to the device. */
	public static final int LAZY_CONTACTS_WRITE_DELAY = 5000;

	/** Well-known pre-shared key for the public group channel. */
	public static final byte[] PUBLIC_GROUP_PSK = MeshcoreUtils.fromHex("8b3387e9c5cdea6ac9e5edbaa115cd72");

	/** How long (ms) a recently-seen ACK is considered current before expiring. */
	public static final long RECENT_ACK_TTL_MS = 30_000L;

	/** Size of an Ed25519 public key in bytes. */
	public static final int PUBKEY_SIZE = 32;

	/** Size of an Ed25519 signature in bytes. */
	public static final int SIGNATURE_SIZE = 64;

	/** Maximum path size in bytes. */
	public static final int MAX_PATH_SIZE = 64;

	/** Maximum on-wire frame size in bytes. */
	public static final int MAX_FRAME_SIZE = 172;

	/** MeshCore companion protocol version implemented by this library. */
	public static final int IMPLEMENTED_PROTOCOL_VERSION = 14;
}
