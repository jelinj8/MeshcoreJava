package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.frames.FrameConstants;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdExportPrivateKey;
import cz.bliksoft.meshcore.frames.cmd.CmdGetAutoaddConfig;
import cz.bliksoft.meshcore.frames.cmd.CmdGetBattAndStorage;
import cz.bliksoft.meshcore.frames.cmd.CmdGetChannel;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContactByKey;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContacts;
import cz.bliksoft.meshcore.frames.cmd.CmdSetChannel;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.frames.push.AdvertPush;
import cz.bliksoft.meshcore.frames.push.ContactDeletedPush;
import cz.bliksoft.meshcore.frames.push.NewAdvertPush;
import cz.bliksoft.meshcore.frames.push.PathUpdatedPush;
import cz.bliksoft.meshcore.frames.resp.AutoaddConfig;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.ContactsStart;
import cz.bliksoft.meshcore.frames.resp.DeviceInfo;
import cz.bliksoft.meshcore.frames.resp.EndOfContacts;
import cz.bliksoft.meshcore.frames.resp.Error;
import cz.bliksoft.meshcore.frames.resp.Ok;
import cz.bliksoft.meshcore.frames.resp.PrivateKey;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class MeshcoreCompanionConfig {
	private static final Logger log = Logger.getLogger(MeshcoreCompanionConfig.class.getName());

	private final MeshcoreCompanion companion;

	public MeshcoreCompanionConfig(MeshcoreCompanion companio) {
		this.companion = companio;
	}

	public void init() throws IOException {
		try {
			companion.sendFrameWithResult(new CmdGetBattAndStorage(), 10000);
			getPrivateKey();
			syncChannels();
		} catch (TimeoutException | InterruptedException e) {
			throw new IOException(e);
		}
		syncContacts(true);
	}

	public void reset() {
		privateKey = null;
		autoaddConfig = null;
	}

	private Map<String, Contact> contacts = null;
	private Map<String, Contact> contactsArchive = new ConcurrentHashMap<>();

	/**
	 * get list of unsaved contacts. Either removed from companion or PushAdvertNew
	 * unsaved.
	 * 
	 * @return
	 */
	public Map<String, Contact> getContactsArchive() {
		return contactsArchive;
	}

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
					String pubkey = MeshcoreUtils.hex(c.getPubkey());
					contacts.put(pubkey, c);
					contactsArchive.remove(pubkey);
				}
					break;
				case PUSH_CONTACT_DELETED: {
					ContactDeletedPush d = (ContactDeletedPush) frame;
					String pubkey = MeshcoreUtils.hex(d.getPubkey());
					Contact c = contacts.remove(pubkey);
					c.saved = false;
					contactsArchive.put(pubkey, c);
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
					contactsArchive.put(MeshcoreUtils.hex(((NewAdvertPush) frame).getPubkey()),
							new Contact(companion, frame.getBytes().clone()));
					break;
				default:
					break;
				}
			}
		};

		companion.registerFrameListener(ContactFrameGroup.class, contactsListener);
	}

	private void refetchContact(byte[] pubkey) {
//		if (contacts == null || !contacts.containsKey(MeshcoreUtils.hex(pubkey)))
//			return;
		try {
			ResponseFrame resp = companion.sendFrameWithResult(new CmdGetContactByKey(pubkey), 2000l);
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
			contacts = new ConcurrentHashMap<>();
		}
		companion.sendFrame(new CmdGetContacts(lastContactsSync));
	}

	/**
	 * Find a saved contact by name (exact match).
	 * 
	 * @param name
	 * @return
	 */
	public Contact getContact(String name) {
		if (name == null || name.length() == 0)
			return null;
		for (Contact c : contacts.values()) {
			if (name.equals(c.getName()))
				return c;
		}
		return null;
	}

	/**
	 * return an unique contact from known contacts, null if not unique or not found
	 * 
	 * @param pubkey
	 * @return
	 */
	public Contact getContact(byte[] pubkey) {
		if (pubkey == null || pubkey.length == 0)
			return null;
		Contact first = null;
		for (Contact c : contacts.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey())) {
				if (first == null) {
					first = c;
				} else {
					return null;
				}
			}
			if (first != null)
				return first;
		}
		for (Contact c : contactsArchive.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey())) {
				if (first == null) {
					first = c;
				} else {
					return null;
				}
			}
			if (first != null)
				return first;
		}
		return null;
	}

	/**
	 * list all contacts with given prefix, optionally limited to type. Try archive
	 * if no saved contacts are found.
	 * 
	 * @param pubkey
	 * @return
	 */
	public List<Contact> findContacts(byte[] pubkey, FrameConstants.AdvertType type) {
		if (pubkey == null || pubkey.length == 0)
			return null;
		List<Contact> result = new ArrayList<>();
		for (Contact c : contacts.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey()) && (type == null || c.getType() == type))
				result.add(c);
		}
		for (Contact c : contactsArchive.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey()) && (type == null || c.getType() == type))
				result.add(c);
		}
		return result;
	}

	private Map<Integer, ChannelInfo> channels = new HashMap<>();

	public void syncChannels() throws IOException, TimeoutException, InterruptedException {
		channels = new HashMap<>();
		for (int i = 0; i < getMaxGroupChannels(); i++) {
			ResponseFrame resp = companion.sendFrameWithResult(new CmdGetChannel(i), 1000);
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
		ResponseFrame cmdResp = companion.sendFrameWithResult(cmd, 1000);
		if (cmdResp instanceof Ok) {
			ResponseFrame resp = companion.sendFrameWithResult(new CmdGetChannel(id), 1000);
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

	/**
	 * fetched in device handshake
	 * 
	 * @return
	 */
	public DeviceInfo getDeviceInfo() {
		return companion.getDeviceInfo();
	}

	/**
	 * fetched in device handshake
	 * 
	 * @return
	 */
	public SelfInfo getSelfInfo() {
		return companion.getSelfInfo();
	}

	public int getProtocolVersion() {
		return getDeviceInfo().getProtocolVersion();
	}

	public int getMaxGroupChannels() {
		return getDeviceInfo().getMaxGroupChannels();
	}

	public int getMaxContacts() {
		return getDeviceInfo().getMaxContacts();
	}

	private final long defaultGetTimeout = 2000;

	private Optional<PrivateKey> privateKey = null;

	/**
	 * Fetch the node's 64-byte Ed25519 private key from the device and cache it.
	 *
	 * @return 64-byte private key, or {@code null} if the device did not respond
	 *         with a valid key
	 */
	public byte[] getPrivateKey() {
		if (privateKey != null) {
			if (privateKey.isPresent())
				return privateKey.get().getPrivateKey();
			else
				return null;
		}
		ResponseFrame resp;
		try {
			resp = companion.sendFrameWithResult(new CmdExportPrivateKey(), defaultGetTimeout);
			if (resp instanceof PrivateKey) {
				privateKey = Optional.of((PrivateKey) resp);
				return ((PrivateKey) resp).getPrivateKey();
			} else {
				privateKey = Optional.empty();
				log.severe("Fetching of private key is DISABLED!");
				return null;
			}
		} catch (IOException | TimeoutException | InterruptedException e) {
			log.log(Level.SEVERE, "Failed to get priovate key", e);
			return null;
		}
	}

	private Optional<AutoaddConfig> autoaddConfig = null;

	public AutoaddConfig getAutoaddConfig() throws IOException, TimeoutException, InterruptedException {
		if (autoaddConfig.isPresent())
			return autoaddConfig.get();

		autoaddConfig = Optional
				.of((AutoaddConfig) companion.sendFrameWithResult(new CmdGetAutoaddConfig(), defaultGetTimeout));
		return autoaddConfig.get();
	}

}
