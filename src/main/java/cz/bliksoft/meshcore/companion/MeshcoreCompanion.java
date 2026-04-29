package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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
import cz.bliksoft.meshcore.frames.cmd.CmdSendChannelData;
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
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;
import cz.bliksoft.meshcore.frames.push.BinaryResponsePush;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.frames.push.MessageWaitingPush;
import cz.bliksoft.meshcore.frames.push.PathDiscoveryResponsePush;
import cz.bliksoft.meshcore.frames.push.SendConfirmedPush;
import cz.bliksoft.meshcore.frames.push.StatusResponsePush;
import cz.bliksoft.meshcore.frames.push.TelemetryResponsePush;
import cz.bliksoft.meshcore.frames.push.TraceDataPush;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.frames.resp.ChannelMsgRecv;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.ContactMsgRecv;
import cz.bliksoft.meshcore.frames.resp.Error;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;
import cz.bliksoft.meshcore.frames.resp.Sent;
import cz.bliksoft.meshcore.frames.resp.SignStart;
import cz.bliksoft.meshcore.frames.resp.Signature;
import cz.bliksoft.meshcore.otaframe.OtaFrame;
import cz.bliksoft.meshcore.otaframe.OtaGroupFrame;
import cz.bliksoft.meshcore.otaframe.OtaUnicastFrame;
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

	private volatile boolean logFramePairing = false;
	private static final int LOG_FRAME_BUFFER_SIZE = 10;
	private final java.util.ArrayDeque<LogRXDataPush> recentLogFrames = new java.util.ArrayDeque<>();

	public void setLogFramePairing(boolean enabled) {
		this.logFramePairing = enabled;
	}

	public boolean isLogFramePairing() {
		return logFramePairing;
	}

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
	 * Send a frame and wait for two stages: first the immediate ACK/SENT response,
	 * then the asynchronous content response (e.g. PUSH_SEND_CONFIRMED). Response
	 * types are taken from the frame's own
	 * {@link CommandFrame#expectedResponses()}.
	 *
	 * @param payload           command frame to send
	 * @param timeoutSendMs     maximum wait for the initial ACK/SENT response (ms)
	 * @param timeoutResponseMs maximum wait for the asynchronous content push (ms)
	 * @return the asynchronous content response frame
	 * @throws IOException          if the transport reports an error
	 * @throws TimeoutException     if either stage times out
	 * @throws InterruptedException if the calling thread is interrupted
	 */
	public ResponseFrame sendFrameWithResultAndResponse(CommandFrame payload, long timeoutSendMs,
			long timeoutResponseMs) throws IOException, TimeoutException, InterruptedException {
		return sendFrameWithResultAndResponse(payload, timeoutSendMs, timeoutResponseMs, payload.expectedResponses());
	}

	/**
	 * Send a frame and wait for two stages using explicit accept codes for the
	 * initial response. See
	 * {@link #sendFrameWithResultAndResponse(CommandFrame, long, long)}.
	 *
	 * @param payload           command frame to send
	 * @param timeoutSendMs     maximum wait for the initial ACK/SENT response (ms)
	 * @param timeoutResponseMs maximum wait for the asynchronous content push (ms)
	 * @param acceptCodes       byte codes of acceptable initial response types
	 * @return the asynchronous content response frame
	 * @throws IOException          if the transport reports an error
	 * @throws TimeoutException     if either stage times out
	 * @throws InterruptedException if the calling thread is interrupted
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
		if (logFramePairing) {
			if (frame instanceof LogRXDataPush) {
				LogRXDataPush lf = (LogRXDataPush) frame;
				lf.getOtaFrame(); // eagerly parse OTA frame on reader thread
				recentLogFrames.addLast(lf);
				while (recentLogFrames.size() > LOG_FRAME_BUFFER_SIZE)
					recentLogFrames.removeFirst();
			} else if (frame instanceof MessageFrameGroup) {
				pairLogFrame((MessageFrameGroup) frame);
			}
		}
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

	private void pairLogFrame(MessageFrameGroup msg) {
		if (msg instanceof ContactMsgRecv) {
			int srcHash = ((ContactMsgRecv) msg).getFrom6()[0] & 0xFF;
			SelfInfo self = getSelfInfo();
			int destHash = (self != null) ? (self.getPubkey()[0] & 0xFF) : -1;
			java.util.Iterator<LogRXDataPush> it = recentLogFrames.descendingIterator();
			while (it.hasNext()) {
				LogRXDataPush lf = it.next();
				OtaFrame ota = lf.getOtaFrame();
				if (ota instanceof OtaUnicastFrame) {
					OtaUnicastFrame uni = (OtaUnicastFrame) ota;
					if (uni.srcHash == srcHash && (destHash == -1 || uni.destHash == destHash)) {
						msg.setPairedLogFrame(lf);
						it.remove();
						return;
					}
				}
			}
		} else if (msg instanceof ChannelMsgRecv) {
			int chIdx = ((ChannelMsgRecv) msg).getChannelIdx();
			ChannelInfo ch = getConfig().getChannel(chIdx);
			if (ch == null)
				return;
			int chHash = ch.getChannelHash();
			java.util.Iterator<LogRXDataPush> it = recentLogFrames.descendingIterator();
			while (it.hasNext()) {
				LogRXDataPush lf = it.next();
				OtaFrame ota = lf.getOtaFrame();
				if (ota instanceof OtaGroupFrame) {
					OtaGroupFrame grp = (OtaGroupFrame) ota;
					if (grp.channelHash == chHash) {
						msg.setPairedLogFrame(lf);
						it.remove();
						return;
					}
				}
			}
		}
	}

	/**
	 * Block until a response frame of the given type and identifying key arrives.
	 * Only works for {@link ResponseFrame} subclasses that override
	 * {@link ResponseFrame#getFrameKey()}.
	 *
	 * @param type    expected response frame type
	 * @param key     identifying value (e.g. hex prefix6 or tag) used to match the
	 *                response
	 * @param timeout maximum wait in milliseconds
	 * @return the matching response frame
	 * @throws InterruptedException if the calling thread is interrupted
	 * @throws IOException          if the connection is lost while waiting
	 * @throws TimeoutException     if no matching response arrives within the
	 *                              timeout
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

	/**
	 * register a listener. Multiple listeners for the same class can be registered.
	 *
	 * @param <T>
	 * @param frameClass key for list to which the listener will be added. Frames
	 *                   assignable to the class will be passed to all listeners in
	 *                   that list.
	 * @param listener   listener to notify, can be registered in multiple lists
	 *                   (when more generic)
	 */
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
	 * clear messages (read them without triggering listeners)
	 */
	public void clearMessages() {
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
	private final AtomicBoolean msgSyncRunning = new AtomicBoolean(false);

	/**
	 * Installs a listener that drains the firmware message queue whenever
	 * PUSH_MSG_WAITING arrives, reading messages until RESP_NO_MORE_MESSAGES.
	 */
	public void installAutosyncMessages() {
		if (msgAutosyncInstalled)
			return;
		msgAutosyncInstalled = true;
		registerFrameListener(MessageWaitingPush.class, frame -> startMessageDrain());
		startMessageDrain();
	}

	/**
	 * Read all queued messages from device, triggering the listeners. Effective
	 * only when the frame reader loop is active.
	 */
	public void startMessageDrain() {
		if (!running.get()) {
			log.warning("Draining messages too soon, reader thread not yet started.");
			return; // reader loop not started yet; PUSH_MSG_WAITING will trigger when ready
		}
		if (!msgSyncRunning.compareAndSet(false, true))
			return;
		Thread t = new Thread(() -> {
			try {
				while (true) {
					ResponseFrame resp = sendFrameWithResult(new CmdSyncNext(), 2000L);
					if (resp.is(ResponseFrameType.RESP_NO_MORE_MESSAGES))
						break;
					// Frame was captured by sendFrameWithResult; re-dispatch to listeners
					// so ChatManager's MessageFrameGroup handler processes it normally.
					final ResponseFrame msg = resp;
					eventExecutor.execute(() -> frameListeners.dispatch(msg));
				}
			} catch (IOException | TimeoutException | InterruptedException e) {
				log.warning("Message drain error: " + e.getMessage());
			} finally {
				msgSyncRunning.set(false);
			}
		}, "meshcore-msg-drain");
		t.setDaemon(true);
		t.start();
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
	 * Send a binary datagram to a group channel. Use pathLen=0xFF for flood.
	 *
	 * @param channelIdx  index into device channel table
	 * @param pathLen     0xFF for flood, otherwise encoded hop count
	 * @param encodedPath encoded path bytes (null when pathLen == 0xFF)
	 * @param dataType    16-bit application-defined data type
	 * @param payload     binary payload
	 */
	public void sendChannelData(int channelIdx, byte pathLen, byte[] encodedPath, int dataType, byte[] payload)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = sendFrameWithResult(
				new CmdSendChannelData(channelIdx, pathLen, encodedPath, dataType, payload), DEFAULT_CMD_TIMEOUT);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
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
	 * Send a keep-alive request (REQ_TYPE_KEEP_ALIVE = 0x02) to a room server. The
	 * server resets push-failure tracking and will re-push any unsynced messages
	 * from {@code syncSince} onwards. The room server replies with a plain ACK (not
	 * a binary-response push), so this method fires and does not block for a
	 * content response.
	 *
	 * @param pubkey32  full 32-byte public key of the room server
	 * @param syncSince epoch-seconds of the last post the client has received; 0 to
	 *                  skip the sync-since hint
	 */
	public void sendKeepAlive(byte[] pubkey32, long syncSince)
			throws IOException, TimeoutException, InterruptedException {
		java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(5).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 0x02); // REQ_TYPE_KEEP_ALIVE
		buf.putInt((int) syncSince);
		sendBinaryReqAsync(pubkey32, buf.array());
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
