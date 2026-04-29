package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.utils.ByteReader;
import cz.bliksoft.meshcore.frames.FrameConstants.AutoAddConfigFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;

/**
 * Reply to CMD_GET_AUTOADD_CONFIG.
 *
 * <p>
 * Frame layout (after response-code byte):
 *
 * <pre>
 *   [1] autoadd_config  – bitmask, see {@link AutoAddConfigFlags}:
 *         bit 0  OVERWRITE_OLDEST  – overwrite oldest non-favourite contact when full
 *         bit 1  CHAT              – auto-add ADV_TYPE_CHAT contacts
 *         bit 2  REPEATER          – auto-add ADV_TYPE_REPEATER contacts
 *         bit 3  ROOM_SERVER       – auto-add ADV_TYPE_ROOM contacts
 *         bit 4  SENSOR            – auto-add ADV_TYPE_SENSOR contacts
 *         bits 5-7                 – reserved
 *   [1] autoadd_max_hops – maximum hop count for auto-add (0–64), present from v1.14+
 * </pre>
 *
 * <p>
 * The per-type bits (1–4) only take effect when manual_add_contacts LSB == 1
 * (manual contact mode). When manual_add_contacts LSB == 0 all contact types
 * are auto-added unconditionally.
 */
public class AutoaddConfig extends ResponseFrame {

	final byte autoaddConfig;
	final int autoAddMaxHops;

	public byte getAutoaddConfig() {
		return autoaddConfig;
	}

	/** Maximum hop count applied during auto-add (0–64). */
	public int getAutoAddMaxHops() {
		return autoAddMaxHops;
	}

	/** Returns {@code true} if the given {@link AutoAddConfigFlags} bit is set. */
	public boolean hasFlag(AutoAddConfigFlags flag) {
		return (autoaddConfig & flag.mask()) != 0;
	}

	public AutoaddConfig(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		autoaddConfig = br.readByte();
		if (data.length > 2) // v14+
			autoAddMaxHops = br.readUnsignedByte();
		else
			autoAddMaxHops = 0;
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_AUTOADD_CONFIG;
	}

	@Override
	public String toString() {
		return String.format(
				"RESP_AUTOADD_CONFIG config=0x%02x (overwriteOldest=%b chat=%b repeater=%b roomServer=%b sensor=%b) maxHops=%d",
				autoaddConfig, hasFlag(AutoAddConfigFlags.OVERWRITE_OLDEST), hasFlag(AutoAddConfigFlags.CHAT),
				hasFlag(AutoAddConfigFlags.REPEATER), hasFlag(AutoAddConfigFlags.ROOM_SERVER),
				hasFlag(AutoAddConfigFlags.SENSOR), autoAddMaxHops);
	}
}
