package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.CompanionErrorException;
import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.FrameListenerRegistry;
import cz.bliksoft.meshcore.Settings;
import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.Frame;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdFactoryReset;
import cz.bliksoft.meshcore.frames.cmd.CmdHasConnection;
import cz.bliksoft.meshcore.frames.cmd.CmdLogout;
import cz.bliksoft.meshcore.frames.cmd.CmdReboot;
import cz.bliksoft.meshcore.frames.cmd.CmdSendAnonReq;
import cz.bliksoft.meshcore.frames.cmd.CmdSendBinaryReq;
import cz.bliksoft.meshcore.frames.cmd.CmdSendChannelTxtMessage;
import cz.bliksoft.meshcore.frames.cmd.CmdSendControlData;
import cz.bliksoft.meshcore.frames.cmd.CmdSendLogin;
import cz.bliksoft.meshcore.frames.cmd.CmdSendPathDiscoveryReq;
import cz.bliksoft.meshcore.frames.cmd.CmdSendRawData;
import cz.bliksoft.meshcore.frames.cmd.CmdSendSelfAdvert;
import cz.bliksoft.meshcore.frames.cmd.CmdSendStatusReq;
import cz.bliksoft.meshcore.frames.cmd.CmdSendTelemetryReq;
import cz.bliksoft.meshcore.frames.cmd.CmdSendTracePath;
import cz.bliksoft.meshcore.frames.cmd.CmdSendTxtMsg;
import cz.bliksoft.meshcore.frames.cmd.CmdSignData;
import cz.bliksoft.meshcore.frames.cmd.CmdSignFinish;
import cz.bliksoft.meshcore.frames.cmd.CmdSignStart;
import cz.bliksoft.meshcore.frames.cmd.CmdSyncNext;
import cz.bliksoft.meshcore.frames.push.MessageWaitingPush;
import cz.bliksoft.meshcore.frames.resp.Error;
import cz.bliksoft.meshcore.frames.push.BinaryResponsePush;
import cz.bliksoft.meshcore.frames.push.PathDiscoveryResponsePush;
import cz.bliksoft.meshcore.frames.push.SendConfirmedPush;
import cz.bliksoft.meshcore.frames.push.StatusResponsePush;
import cz.bliksoft.meshcore.frames.push.TelemetryResponsePush;
import cz.bliksoft.meshcore.frames.push.TraceDataPush;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.Sent;
import cz.bliksoft.meshcore.frames.resp.Signature;
import cz.bliksoft.meshcore.frames.resp.SignStart;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

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

	private static final long DEFAULT_CMD_TIMEOUT = 2000L;
	private static final long DEFAULT_MSG_TIMEOUT = 30000L;

	/** Reboot the device. Fire-and-forget — no response expected. */
	public void reboot() throws IOException {
		sendFrame(new CmdReboot());
	}

	/**
	 * @return true if the device currently has an active connection to the contact
	 */
	public boolean hasConnection(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdHasConnection(pubkey), DEFAULT_CMD_TIMEOUT);
		return !(resp instanceof Error);
	}

	/**
	 * Send the device's own advert over the mesh.
	 *
	 * @param method {@link CmdSendSelfAdvert.AdvertMethod#FLOOD} or
	 *               {@code ZERO_HOP}
	 */
	public void sendSelfAdvert(CmdSendSelfAdvert.AdvertMethod method)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendSelfAdvert(method), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/** Send a direct text message and wait for delivery confirmation. */
	public SendConfirmedPush sendTxtMsg(MessageTextType txtType, byte[] prefix6, Integer attempt, Long timestamp,
			String text) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(
				new CmdSendTxtMsg(txtType, prefix6, attempt, timestamp, text), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (SendConfirmedPush) resp;
	}

	/**
	 * Send a direct text message and return {@link Sent} immediately without
	 * waiting for delivery confirmation.
	 */
	public Sent sendTxtMsgAsync(MessageTextType txtType, byte[] prefix6, Integer attempt, Long timestamp, String text)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendTxtMsg(txtType, prefix6, attempt, timestamp, text),
				DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Send a direct text message with up to 3 attempts. The first two attempts use
	 * whatever routing the device has cached (direct if a path is known, flood
	 * otherwise). If both time out, the contact's cached path is reset so the third
	 * and final attempt is forced to use flood routing.
	 *
	 * @param txtType   message text type
	 * @param prefix6   6-byte public key prefix identifying the recipient
	 * @param timestamp Unix epoch seconds, or {@code null} to use the current time
	 * @param text      message text
	 * @return {@link SendConfirmedPush} delivery confirmation
	 * @throws IOException
	 * @throws TimeoutException     if all 3 attempts time out
	 * @throws InterruptedException
	 */
	public SendConfirmedPush sendTxtMsgWithRetry(MessageTextType txtType, byte[] prefix6, Long timestamp, String text)
			throws IOException, TimeoutException, InterruptedException {
		for (int attempt = 0; attempt < 2; attempt++) {
			try {
				return sendTxtMsg(txtType, prefix6, attempt, timestamp, text);
			} catch (TimeoutException e) {
				log.warning(String.format("sendTxtMsg attempt %d timed out for %s", attempt,
						MeshcoreUtils.hexPrefix6(prefix6)));
			}
		}
		// Final attempt: reset path to force flood routing
		Contact contact = config.getContact(prefix6);
		if (contact != null)
			config.resetPath(contact.getPubkey());
		return sendTxtMsg(txtType, prefix6, 2, timestamp, text);
	}

	/**
	 * Send a text message to a group channel.
	 */
	public ResponseFrame sendChannelTxtMessage(MessageTextType txtType, int channelId, Long timestamp, String text)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendChannelTxtMessage(txtType, channelId, timestamp, text),
				DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return resp;
	}

	/**
	 * Send raw data directly via a known path.
	 */
	public void sendRawData(int pathLen, byte[] path, byte[] data)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendRawData(pathLen, path, data), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/** Send a binary request and wait for the binary response push. */
	public BinaryResponsePush sendBinaryReq(byte[] pubkey32, byte[] data)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(new CmdSendBinaryReq(pubkey32, data), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (BinaryResponsePush) resp;
	}

	/**
	 * Send a binary request and return {@link Sent} immediately without waiting for
	 * the response push.
	 */
	public Sent sendBinaryReqAsync(byte[] pubkey32, byte[] data)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendBinaryReq(pubkey32, data), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Send an anonymous request. Returns RESP_SENT; no async response is defined.
	 */
	public Sent sendAnonReq(byte[] pubkey, byte[] msgData) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendAnonReq(pubkey, msgData), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/** Send a status request and wait for the status response push. */
	public StatusResponsePush sendStatusReq(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(new CmdSendStatusReq(pubkey), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (StatusResponsePush) resp;
	}

	/**
	 * Send a status request and return {@link Sent} immediately without waiting for
	 * the response push.
	 */
	public Sent sendStatusReqAsync(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendStatusReq(pubkey), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Send a telemetry request and wait for the response push. Pass {@code null}
	 * for self-telemetry (no async response).
	 */
	public TelemetryResponsePush sendTelemetryReq(byte[] pubkey)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(new CmdSendTelemetryReq(pubkey), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (TelemetryResponsePush) resp;
	}

	/**
	 * Send a telemetry request and return {@link Sent} immediately without waiting
	 * for the response push.
	 */
	public Sent sendTelemetryReqAsync(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendTelemetryReq(pubkey), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/** Send a trace-path packet and wait for the trace data push. */
	public TraceDataPush sendTracePath(byte[] tag, long auth, byte flags, byte[] path)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(new CmdSendTracePath(tag, auth, flags, path),
				DEFAULT_CMD_TIMEOUT, DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (TraceDataPush) resp;
	}

	/**
	 * Send a trace-path packet and return {@link Sent} immediately without waiting
	 * for the trace data push.
	 */
	public Sent sendTracePathAsync(byte[] tag, long auth, byte flags, byte[] path)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendTracePath(tag, auth, flags, path), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Trigger path-discovery for a contact and wait for the discovery response
	 * push.
	 */
	public PathDiscoveryResponsePush sendPathDiscoveryReq(byte[] pubkey)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResultAndResponse(new CmdSendPathDiscoveryReq(pubkey), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (PathDiscoveryResponsePush) resp;
	}

	/**
	 * Trigger path-discovery and return {@link Sent} immediately without waiting
	 * for the discovery response push.
	 */
	public Sent sendPathDiscoveryReqAsync(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendPathDiscoveryReq(pubkey), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Send control data (first byte must have bit 7 set).
	 */
	public void sendControlData(byte[] data) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendControlData(data), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/**
	 * Log in to a room server. Returns the login success/fail push.
	 */
	public ResponseFrame login(byte[] pubkey, String password)
			throws IOException, TimeoutException, InterruptedException {
		return sendFrameWithResultAndResponse(new CmdSendLogin(pubkey, password), DEFAULT_CMD_TIMEOUT,
				DEFAULT_MSG_TIMEOUT);
	}

	/**
	 * Send a login request and return {@link Sent} immediately without waiting for
	 * login success/fail push.
	 */
	public Sent loginAsync(byte[] pubkey, String password) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdSendLogin(pubkey, password), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Sent) resp;
	}

	/**
	 * Disconnect from a room server.
	 */
	public void logout(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(new CmdLogout(pubkey), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/**
	 * Sign data using the device's private key.
	 *
	 * @param data data to sign; must fit within the max length reported by the
	 *             device
	 * @return 64-byte Ed25519 signature
	 */
	public byte[] sign(byte[] data) throws IOException, TimeoutException, InterruptedException {
		SignStart signStart = (SignStart) sendFrameWithResult(new CmdSignStart(), DEFAULT_CMD_TIMEOUT);
		if (data.length > signStart.getMaxDataLength())
			throw new IllegalArgumentException(
					"Data too large to sign: " + data.length + " > " + signStart.getMaxDataLength());
		ResponseFrame dataResp = sendFrameWithResult(new CmdSignData(data), DEFAULT_CMD_TIMEOUT);
		if (dataResp instanceof Error)
			throw new CompanionErrorException("Sign data rejected: " + dataResp);
		ResponseFrame finishResp = sendFrameWithResult(new CmdSignFinish(), DEFAULT_CMD_TIMEOUT);
		if (finishResp instanceof Error)
			throw new CompanionErrorException("Sign finish failed: " + finishResp);
		return ((Signature) finishResp).getSignature();
	}

}
