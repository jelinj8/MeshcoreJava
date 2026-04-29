package cz.bliksoft.meshcore.frames.cmd;

import java.time.Instant;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class CmdSendTxtMsg extends CommandsWithSentResponse {

	/**
	 * Maximum UTF-8 bytes for the message text. The serial frame header accounts
	 * for 13 bytes, but additional lower-level mesh/LoRa framing further reduces
	 * usable payload. Value matches the official companion app limit. Room/repeater
	 * contacts share this limit (their re-signed retransmit fits within it).
	 */
	public static final int MAX_TEXT_BYTES = 150;

	final MessageTextType txtType;
	final int attempt;
	final long timestamp;
	final byte[] pubKeyPrefix;
	final String text;

	/**
	 * @param txtType   message text type (plain, signed, etc.)
	 * @param prefix6   6-byte public key prefix of the recipient
	 * @param attempt   retry attempt counter (0 for first send); pass {@code null}
	 *                  for 0
	 * @param timestamp Unix epoch seconds; pass {@code null} to use the current
	 *                  time
	 * @param text      message text (must fit within {@link #MAX_TEXT_BYTES} UTF-8
	 *                  bytes)
	 */
	public CmdSendTxtMsg(MessageTextType txtType, byte[] prefix6, Integer attempt, Long timestamp, String text) {
		this.txtType = txtType;
		this.attempt = (attempt == null ? 0 : attempt);
		this.pubKeyPrefix = prefix6;
		this.timestamp = (timestamp != null ? timestamp : Instant.now().getEpochSecond());
		this.text = text;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SEND_TXT_MSG;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		// type
		bb.put(getTypeCode());
		// txt_type
		bb.put(txtType.code());
		// attempt
		bb.put((byte) attempt);
		// timestamp
		bb.putUInt32LE(timestamp);
		// prefix6
		bb.put(pubKeyPrefix, 6);
		// message text
		bb.put(text);
		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_SENT;
	}

	@Override
	public ResponseFrameType getExpectedResponseFrameType() {
		return ResponseFrameType.PUSH_SEND_CONFIRMED;
	}

	@Override
	public String toString() {
		return String.format("%s %s %d@%s->%s", getFrameType(), txtType, attempt, MeshcoreUtils.hex(pubKeyPrefix),
				text);
	}
}
