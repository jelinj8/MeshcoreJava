package cz.bliksoft.meshcore;

import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class Settings {
	public static final int SEND_TIMEOUT_BASE_MILLIS = 500;
	public static final float FLOOD_SEND_TIMEOUT_FACTOR = 16.0f;
	public static final float DIRECT_SEND_PERHOP_FACTOR = 6.0f;
	public static final int DIRECT_SEND_PERHOP_EXTRA_MILLIS = 250;
	public static final int LAZY_CONTACTS_WRITE_DELAY = 5000;

	public static final byte[] PUBLIC_GROUP_PSK = MeshcoreUtils.fromHex("8b3387e9c5cdea6ac9e5edbaa115cd72");

	public static final long RECENT_ACK_TTL_MS = 30_000L;

	public static final int PUBKEY_SIZE = 32;
	public static final int MAX_PATH_SIZE = 64;

	public static final int MAX_FRAME_SIZE = 172;
	
	public static final int IMPLEMENTED_PROTOCOL_VERSION = 14;
}
