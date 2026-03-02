package cz.bliksoft.meshcore.frames.cmd;

import java.time.Instant;

import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.group.CommandsWithSentResponse;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSendTxtMsg extends CommandsWithSentResponse {

	final MessageTextType txtType;
	final int attempt;
	final long timestamp;
	final byte[] pubKeyPrefix;
	final String text;

	/**
	 * 
	 * @param txtType
	 * @param prefix6
	 * @param attempt   default 0
	 * @param timestamp default getEpochSecond()
	 * @param text
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

}
