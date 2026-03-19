package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import cz.bliksoft.meshcore.frames.FrameConstants;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdGetBattAndStorage;
import cz.bliksoft.meshcore.frames.cmd.CmdGetChannel;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContactByKey;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContacts;
import cz.bliksoft.meshcore.frames.cmd.CmdSetChannel;
import cz.bliksoft.meshcore.frames.cmd.CmdSyncNext;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.frames.push.AdvertPush;
import cz.bliksoft.meshcore.frames.push.ContactDeletedPush;
import cz.bliksoft.meshcore.frames.push.MessageWaitingPush;
import cz.bliksoft.meshcore.frames.push.NewAdvertPush;
import cz.bliksoft.meshcore.frames.push.PathUpdatedPush;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.ContactsStart;
import cz.bliksoft.meshcore.frames.resp.EndOfContacts;
import cz.bliksoft.meshcore.frames.resp.Error;
import cz.bliksoft.meshcore.frames.resp.Ok;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public abstract class MeshcoreCompanion extends MeshcoreCompanionBase {

	public MeshcoreCompanion(String name) {
		super(name);
	}

	private static final Logger log = Logger.getLogger(MeshcoreCompanion.class.getName());

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
		try {
			sendFrameWithResult(new CmdGetBattAndStorage(), 10000);
			syncChannels();
		} catch (TimeoutException | InterruptedException e) {
			throw new IOException(e);
		}
		syncContacts(true);
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
			ResponseFrame resp = sendFrameWithResult(new CmdSyncNext(), 500l);
			while (!resp.is(ResponseFrameType.RESP_ERR) && !resp.is(ResponseFrameType.RESP_NO_MORE_MESSAGES)) {
				resp = sendFrameWithResult(new CmdSyncNext(), 500l);
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

	private Map<String, Contact> contacts = null;
	Long lastContactsSync = null;
	boolean contactsSyncInstalled = false;

	private void installSyncContacts() {
		if (contactsSyncInstalled)
			return;
		contactsSyncInstalled = true;
		FrameListener<ContactFrameGroup> contactsListener = new FrameListener<ContactFrameGroup>() {

			long expectedCount = 0;

			@Override
			public void onFrame(ContactFrameGroup frame) {
				switch (frame.getFrameType()) {
				case RESP_CONTACT: {
					Contact c = (Contact) frame;
					contacts.put(MeshcoreUtils.hex(c.getPubkey()), c);
				}
					break;
				case PUSH_CONTACT_DELETED: {
					ContactDeletedPush d = (ContactDeletedPush) frame;
					contacts.remove(MeshcoreUtils.hex(d.getPubkey()));
				}
					break;
				case PUSH_CONTACTS_FULL:
					break;
				case RESP_END_OF_CONTACTS: {
					EndOfContacts eoc = (EndOfContacts) frame;
					if (eoc.getLastUpdated() > 0)
						lastContactsSync = eoc.getLastUpdated();

					if (contacts.size() != expectedCount) {
						log.severe("Contacts count does not match!");
					}
				}
					break;
				case RESP_CONTACTS_START: {
					expectedCount = ((ContactsStart) frame).getContactsCount();
				}
					break;
				case PUSH_PATH_UPDATED:
					refetchContact(((PathUpdatedPush) frame).getPubkey());
					break;
				case PUSH_ADVERT:
					refetchContact(((AdvertPush) frame).getPubkey());
					break;
				case PUSH_NEW_ADVERT:
					// Add contact to local DB without adding it to companion's storage
					contacts.put(MeshcoreUtils.hex(((NewAdvertPush) frame).getPubkey()),
							new Contact(MeshcoreCompanion.this, frame.getBytes().clone()));
					break;
				default:
					break;
				}
			}
		};

		registerFrameListener(ContactFrameGroup.class, contactsListener);
	}

	private void refetchContact(byte[] pubkey) {
//		if (contacts == null || !contacts.containsKey(MeshcoreUtils.hex(pubkey)))
//			return;
		try {
			ResponseFrame resp = sendFrameWithResult(new CmdGetContactByKey(pubkey), 2000l);
			if (resp instanceof Error) {
				log.severe(String.format("Contact refetch error for %s: %s", MeshcoreUtils.hex(pubkey),
						((Error) resp).getCode()));
			}
			// updated RESP_CONTACT is handled by contactsListener above
		} catch (IOException | TimeoutException | InterruptedException e) {
			log.log(Level.SEVERE, "Contact refetch exception", e);
		}
	}

	public void syncContacts(boolean full) throws IOException {
		installSyncContacts();
		if (full || lastContactsSync == null) {
			lastContactsSync = null;
			contacts = new HashMap<>();
		}
		sendFrame(new CmdGetContacts(lastContactsSync));
	}

	public Contact getContact(String name) {
		if (name == null || name.length() == 0)
			return null;
		for (Contact c : contacts.values()) {
			if (name.equals(c.getName()))
				return c;
		}
		return null;
	}

	public Contact getContact(byte[] pubkey) {
		if (pubkey == null || pubkey.length == 0)
			return null;
		for (Contact c : contacts.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey()))
				return c;
		}
		return null;
	}

	/**
	 * list all contacts with given prefix, optionally limited to type
	 * 
	 * @param pubkey
	 * @return
	 */
	public List<Contact> getContacts(byte[] pubkey, FrameConstants.AdvertType type) {
		if (pubkey == null || pubkey.length == 0)
			return null;
		List<Contact> result = new ArrayList<>();
		for (Contact c : contacts.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey()) && (type == null || c.getType() == type))
				result.add(c);
		}
		return result;
	}

	private Map<Integer, ChannelInfo> channels = new HashMap<>();

	public void syncChannels() throws IOException, TimeoutException, InterruptedException {
		channels = new HashMap<>();
		for (int i = 0; i < maxChannels; i++) {
			ResponseFrame resp = sendFrameWithResult(new CmdGetChannel(i), 1000);
			if (resp instanceof ChannelInfo) {
				ChannelInfo chI = (ChannelInfo) resp;
				if (chI.getName().length() > 0) {
					channels.put(i, chI);
				}
			}
		}
	}

	public void setChannel(int id, String name, byte[] key) throws IOException, TimeoutException, InterruptedException {
		CmdSetChannel cmd = new CmdSetChannel(id, name, key);
		ResponseFrame cmdResp = sendFrameWithResult(cmd, 1000);
		if (cmdResp instanceof Ok) {
			ResponseFrame resp = sendFrameWithResult(new CmdGetChannel(id), 1000);
			if (resp instanceof ChannelInfo) {
				ChannelInfo chI = (ChannelInfo) resp;
				if (chI.getName().length() > 0) {
					channels.put(id, chI);
				} else {
					channels.remove(id);
				}
			}
		} else {
			throw new IOException(
					String.format("Failed to set group channel %d=%s: %s", id, name, ((Error) cmdResp).getCode()));
		}
	}

	public ChannelInfo getChannel(int id) {
		return channels.get(id);
	}

	public Collection<ChannelInfo> getChannels() {
		return Collections.unmodifiableCollection(channels.values());
	}

	public ChannelInfo getChannel(String name) {
		if (name == null || name.length() == 0)
			return null;
		for (ChannelInfo c : channels.values()) {
			if (name.equals(c.getName()))
				return c;
		}
		return null;
	}
}
