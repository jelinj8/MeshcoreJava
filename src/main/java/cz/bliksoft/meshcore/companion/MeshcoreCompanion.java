package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.FrameListenerRegistry;
import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.Frame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdFactoryReset;
import cz.bliksoft.meshcore.frames.cmd.CmdGetAdvertPath;
import cz.bliksoft.meshcore.frames.cmd.CmdSyncNext;
import cz.bliksoft.meshcore.frames.push.MessageWaitingPush;

public abstract class MeshcoreCompanion extends MeshcoreCompanionBase {

	public MeshcoreCompanion(String name) {
		super(name);
		config = new MeshcoreCompanionConfig(this);
	}

	private static final Logger log = Logger.getLogger(MeshcoreCompanion.class.getName());

	private final MeshcoreCompanionConfig config;

	public MeshcoreCompanionConfig getConfig() {
		return config;
	}

	/**
	 * registry for listeners that will be notified about frame received from device
	 */
	private FrameListenerRegistry frameListeners = new FrameListenerRegistry();

	private final java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<ResponseFrame>> responseWaiters = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<String, ResponseFrame> recentResponses = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	protected void failAllWaiters(IOException ex) {
		super.failAllWaiters(ex);

		responseWaiters.forEach((k, fut) -> fut.completeExceptionally(ex));
		responseWaiters.clear();
		recentResponses.clear();
	}

	@Override
	protected void deviceInit() throws IOException {
		config.init();
	}

	/**
	 * blocking call, list of expected response types from CommandFrame definition.
	 * 
	 * @param payload
	 * @param timeoutMs
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public ResponseFrame sendFrameWithResultAndResponse(CommandFrame payload, long timeoutSendMs,
			long timeoutResponseMs) throws IOException, TimeoutException, InterruptedException {
		return sendFrameWithResultAndResponse(payload, timeoutSendMs, timeoutResponseMs, payload.expectedResponses());
	}

	/**
	 * blocking call, waits for specified response frame types.
	 * 
	 * @param payload
	 * @param timeoutSendMs
	 * @param acceptCodes
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public ResponseFrame sendFrameWithResultAndResponse(CommandFrame payload, long timeoutSendMs,
			long timeoutResponseMs, byte... acceptCodes) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(payload, timeoutSendMs, acceptCodes);

		if (resp.is(ResponseFrameType.RESP_ERR)) {
			return resp;
		}

		ResponseFrameType responseType = payload.getExpectedResponseFrameType();
		if (responseType != null) {
			String key = payload.getResultKey(resp);
			if (key != null) {
				Long expectedTimeout = payload.getExpectedResponseTimeout();
				if (expectedTimeout == null || expectedTimeout <= 0)
					expectedTimeout = timeoutResponseMs;

				return waitForResponseFrame(responseType, key, Math.max(expectedTimeout, timeoutResponseMs));
			} else {
				log.fine(String.format("No message key specified for %s", payload.getFrameType()));
				return null;
			}
		} else {
			log.warning(String.format("Sending frame %s with wait for response without defined expected response type!",
					payload.getFrameType()));
			return null;
		}
	}

	protected boolean checkWaiting(ResponseFrame frame) {
		String key = frame.getFrameKey();
		if (key != null) {
			recentResponses.put(key, frame);
			// prune occasionally
			if (recentResponses.size() > 128) {
				long now = System.currentTimeMillis();
				recentResponses.entrySet()
						.removeIf(e -> now - e.getValue().getReceivedAt() > Settings.RECENT_ACK_TTL_MS);
			}

			CompletableFuture<ResponseFrame> future = responseWaiters.remove(key);

			// special case for LOGIN_FAIL
			if (future == null && frame.getFrameType() == ResponseFrameType.PUSH_LOGIN_FAIL) {
				String successKey = ResponseFrame.getFrameKey(ResponseFrameType.PUSH_LOGIN_SUCCESS,
						frame.getResponseKey());
				future = responseWaiters.remove(successKey);
			}

			if (future != null) {
				future.complete(frame);
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean dispatchFrame(ResponseFrame frame) throws IOException {
		// blocking calls
		if (!super.dispatchFrame(frame)) {
			// check waiting futures
			if (!checkWaiting(frame)) {
				// dispatch listeners
				eventExecutor.execute(() -> {
					frameListeners.dispatch(frame);
				});
			} else
				return true; // consumed by waiting future
		} else
			return true; // consumed by sendAndAwait

		return false; // not consumed
	}

	/**
	 * waits for a response frame of proper type and identification. Only for
	 * {@link ResponseFrame} with overriden getFrameKey.
	 * 
	 * @param type
	 * @param key     value identifying response frame (prefix6, tag...)
	 * @param timeout
	 * @return ResponseFrame to be processed
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public ResponseFrame waitForResponseFrame(ResponseFrameType type, String key, long timeout)
			throws InterruptedException, IOException, TimeoutException {
		String respKey = ResponseFrame.getFrameKey(type, key);

		ResponseFrame resp = recentResponses.remove(respKey);
		if (resp != null) {
			return resp;
		}

		CompletableFuture<ResponseFrame> waiter = new CompletableFuture<>();
		CompletableFuture<ResponseFrame> prev = responseWaiters.putIfAbsent(respKey, waiter);
		if (prev != null)
			waiter = prev;

		try {
			return waiter.get(timeout, TimeUnit.MILLISECONDS);
		} catch (ExecutionException ee) {
			Throwable c = ee.getCause();
			if (c instanceof IOException)
				throw (IOException) c;
			throw new IOException(String.format("Wait for %s[%s] failed", type, respKey), c);
		} catch (java.util.concurrent.TimeoutException te) {
			throw new TimeoutException(String.format("Wait for %s[%s] timed-out", type, respKey));
		} finally {
			responseWaiters.remove(respKey, waiter);
		}
	}

	public <T extends Frame> void registerFrameListener(Class<T> frameClass, FrameListener<? super T> listener) {
		frameListeners.register(frameClass, listener);
	}

	public <T extends Frame> void removeFrameListener(Class<T> frameClass, FrameListener<? super T> listener) {
		frameListeners.remove(frameClass, listener);
	}

	public void removeFrameListener(FrameListener<?> listener) {
		frameListeners.removeFrameListener(listener);
	}

	/**
	 * drain messages (read them without triggering listeners)
	 */
	public void drainMessages() {
		try {
			ResponseFrame resp = sendFrameWithResult(new CmdSyncNext(), 1000l);
			while (!resp.is(ResponseFrameType.RESP_ERR) && !resp.is(ResponseFrameType.RESP_NO_MORE_MESSAGES)) {
				resp = sendFrameWithResult(new CmdSyncNext(), 1000l);
			}
		} catch (IOException | TimeoutException | InterruptedException e) {
			log.log(Level.SEVERE, "draining messages", e);
		}
	}

	private boolean msgAutosyncInstalled = false;

	/**
	 * installs default listeners, triggering reading of messages when received
	 */
	public void installAutosyncMessages() {
		if (msgAutosyncInstalled)
			return;
		msgAutosyncInstalled = true;
		FrameListener<ResponseFrame> msgReader = new FrameListener<ResponseFrame>() {
			@Override
			public void onFrame(ResponseFrame frame) {
				try {
					switch (frame.getFrameType()) {
					case PUSH_MSG_WAITING:
						sendFrame(new CmdSyncNext());
						break;
					default:
						break;
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, "msgReader listener error", e);
				}
			}
		};

		registerFrameListener(MessageWaitingPush.class, msgReader);
	}

	/**
	 * perform a device factory reset
	 */
	public void factoryReset() {
		try {
			sendFrame(new CmdFactoryReset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		config.reset();
	}

	
}
