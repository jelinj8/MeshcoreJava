package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.resp.Sent;

/**
 * Base class for command frames whose final delivery result arrives as a
 * follow-up response keyed on the {@link Sent} acknowledgement.
 *
 * <p>
 * After the device acknowledges a send with a {@link Sent} frame, this class
 * extracts the result key used to match the subsequent delivery- confirmation
 * push, and derives the appropriate wait timeout from the {@link Sent} frame's
 * expected-timeout field.
 * </p>
 */
public abstract class CommandsWithSentResponse extends CommandFrame {

	protected Sent callResult;

	/**
	 * Records the {@link Sent} acknowledgement and returns the result key used to
	 * match the follow-up delivery-confirmation response.
	 *
	 * @param callResult the {@link Sent} response frame from the device
	 * @return the result key to wait for
	 */
	public String getResultKey(ResponseFrame callResult) {
		this.callResult = (Sent) callResult;
		return this.callResult.getResponseKey();
	}

	/**
	 * Returns the timeout (ms) to wait for the follow-up delivery confirmation, as
	 * specified by the {@link Sent} acknowledgement.
	 *
	 * @return timeout in milliseconds, or {@code null} if the sent frame has not
	 *         yet been received
	 */
	public Long getExpectedResponseTimeout() {
		return callResult.getExpectedTimeout();
	}

}
