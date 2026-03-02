package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.resp.Sent;

public abstract class CommandsWithSentResponse extends CommandFrame {

	protected Sent callResult;

	public String getResultKey(ResponseFrame callResult) {
		this.callResult = (Sent) callResult;
		return this.callResult.getResponseKey();
	}

	public Long getExpectedResponseTimeout() {
		return callResult.getExpectedTimeout();
	}

}
