package cz.bliksoft.meshcore.companion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import cz.bliksoft.meshcore.CompanionErrorException;
import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.frames.FrameConstants;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertLocPolicy;
import cz.bliksoft.meshcore.frames.FrameConstants.AutoAddConfigFlags;
import cz.bliksoft.meshcore.frames.FrameConstants.StatsCommandFrameSubtype;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdExportContact;
import cz.bliksoft.meshcore.frames.cmd.CmdExportPrivateKey;
import cz.bliksoft.meshcore.frames.cmd.CmdGetAdvertPath;
import cz.bliksoft.meshcore.frames.cmd.CmdGetAutoaddConfig;
import cz.bliksoft.meshcore.frames.cmd.CmdGetBattAndStorage;
import cz.bliksoft.meshcore.frames.cmd.CmdGetChannel;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContactByKey;
import cz.bliksoft.meshcore.frames.cmd.CmdGetContacts;
import cz.bliksoft.meshcore.frames.cmd.CmdGetCustomVars;
import cz.bliksoft.meshcore.frames.cmd.CmdGetDeviceTime;
import cz.bliksoft.meshcore.frames.cmd.CmdGetStats;
import cz.bliksoft.meshcore.frames.cmd.CmdGetTuningParams;
import cz.bliksoft.meshcore.frames.cmd.CmdImportContact;
import cz.bliksoft.meshcore.frames.cmd.CmdImportPrivateKey;
import cz.bliksoft.meshcore.frames.cmd.CmdRemoveContact;
import cz.bliksoft.meshcore.frames.cmd.CmdResetPath;
import cz.bliksoft.meshcore.frames.cmd.CmdSetAdvertLatLon;
import cz.bliksoft.meshcore.frames.cmd.CmdSetAdvertName;
import cz.bliksoft.meshcore.frames.cmd.CmdSetAutoaddConfig;
import cz.bliksoft.meshcore.frames.cmd.CmdSetChannel;
import cz.bliksoft.meshcore.frames.cmd.CmdSetCustomVar;
import cz.bliksoft.meshcore.frames.cmd.CmdSetDevicePin;
import cz.bliksoft.meshcore.frames.cmd.CmdSetDeviceTime;
import cz.bliksoft.meshcore.frames.cmd.CmdSetFloodScope;
import cz.bliksoft.meshcore.frames.cmd.CmdSetOtherParams;
import cz.bliksoft.meshcore.frames.cmd.CmdSetPathHashMode;
import cz.bliksoft.meshcore.frames.cmd.CmdSetRadioParams;
import cz.bliksoft.meshcore.frames.cmd.CmdSetRadioTXPower;
import cz.bliksoft.meshcore.frames.cmd.CmdSetTuningParams;
import cz.bliksoft.meshcore.frames.cmd.CmdShareContact;
import cz.bliksoft.meshcore.frames.group.ContactFrameGroup;
import cz.bliksoft.meshcore.frames.push.AdvertPush;
import cz.bliksoft.meshcore.frames.push.ContactDeletedPush;
import cz.bliksoft.meshcore.frames.push.NewAdvertPush;
import cz.bliksoft.meshcore.frames.push.PathUpdatedPush;
import cz.bliksoft.meshcore.frames.resp.AdvertPath;
import cz.bliksoft.meshcore.frames.resp.AutoaddConfig;
import cz.bliksoft.meshcore.frames.resp.BattAndStorage;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.ContactsStart;
import cz.bliksoft.meshcore.frames.resp.CurrTime;
import cz.bliksoft.meshcore.frames.resp.CustomVars;
import cz.bliksoft.meshcore.frames.resp.DeviceInfo;
import cz.bliksoft.meshcore.frames.resp.EndOfContacts;
import cz.bliksoft.meshcore.frames.resp.Error;
import cz.bliksoft.meshcore.frames.resp.ExportContact;
import cz.bliksoft.meshcore.frames.resp.Ok;
import cz.bliksoft.meshcore.frames.resp.PrivateKey;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;
import cz.bliksoft.meshcore.frames.resp.Stats;
import cz.bliksoft.meshcore.frames.resp.TuningParams;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class MeshcoreCompanionConfig {
	private static final Logger log = Logger.getLogger(MeshcoreCompanionConfig.class.getName());

	private final MeshcoreCompanion companion;

	public MeshcoreCompanionConfig(MeshcoreCompanion companion) {
		this.companion = companion;
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
		customVars = null;
		tuningParams = null;
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
		}
		if (first != null)
			return first;
		for (Contact c : contactsArchive.values()) {
			if (MeshcoreUtils.isPrefix(pubkey, c.getPubkey())) {
				if (first == null) {
					first = c;
				} else {
					return null;
				}
			}
		}
		if (first != null)
			return first;
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
			} else {
				throw new CompanionErrorException(resp.toString());
			}
		}
	}

	public void syncChannel(int id) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdGetChannel(id), 1000);
		if (resp instanceof ChannelInfo) {
			ChannelInfo chI = (ChannelInfo) resp;
			if (chI.getName().length() > 0) {
				channels.put(id, chI);
			}
		} else {
			throw new CompanionErrorException(resp.toString());
		}
	}

	/**
	 * Set a channel in a slot. Set name to null to remove the channel (zero the
	 * key).
	 * 
	 * @param id
	 * @param name
	 * @param key
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setChannel(int id, String name, byte[] key) throws IOException, TimeoutException, InterruptedException {
		if (name != null) {
			ChannelInfo existing = getChannel(name);
			if ((existing != null) && (id != existing.getId()))
				throw new CompanionErrorException(
						String.format("Creating a channel with already existing name %d:%s, but different ID (%d)!",
								existing.getId(), name, id));
		}
		CmdSetChannel cmd = new CmdSetChannel(id, name, (name != null ? key : new byte[16]));
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
			throw new CompanionErrorException(
					String.format("Failed to set group channel %d=%s: %s", id, name, ((Error) cmdResp).getCode()));
		}
	}

	/**
	 * add a channel to a free slot or update a channel with the same name if the
	 * key is different
	 * 
	 * @param name
	 * @param key
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public int setChannel(String name, byte[] key) throws IOException, TimeoutException, InterruptedException {
		syncChannels();
		Integer idxToSet = null;
		ChannelInfo chI = getChannel(name);

		if (chI != null)
			idxToSet = chI.getId();
		else {
			int i = 1;
			while (channels.containsKey(i))
				i++;
			if (i < getMaxGroupChannels())
				idxToSet = i;
		}

		if (idxToSet != null) {
			if (chI == null || !Arrays.equals(chI.getPubkey(), key)) {
				setChannel(idxToSet, name, key);
				syncChannel(idxToSet);
			}
			return idxToSet;
		} else {
			throw new CompanionErrorException("No slot available");
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
	 * @throws UnsupportedOperationException when disabled in device firmware
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
				throw new UnsupportedOperationException("Fetching of private key is DISABLED!");
			}
		} catch (IOException | TimeoutException | InterruptedException e) {
			log.log(Level.SEVERE, "Failed to get private key", e);
			return null;
		}
	}

	/**
	 * import a new private key to the device.
	 * 
	 * @param key
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws UnsupportedOperationException when disabled in device firmware
	 */
	public void importPrivateKey(byte[] key) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdImportPrivateKey(key), defaultGetTimeout);
		if (resp instanceof Error) {
			throw new UnsupportedOperationException("Private key import is disabled on the device.");
		}
		privateKey = null;
	}

	private AutoaddConfig autoaddConfig = null;

	public AutoaddConfig getAutoaddConfig() throws IOException, TimeoutException, InterruptedException {
		if (autoaddConfig != null)
			return autoaddConfig;

		autoaddConfig = (AutoaddConfig) companion.sendFrameWithResult(new CmdGetAutoaddConfig(), defaultGetTimeout);
		return autoaddConfig;
	}

	private CustomVars customVars = null;

	public CustomVars getCustomVars() throws IOException, TimeoutException, InterruptedException {
		if (customVars != null)
			return customVars;

		customVars = (CustomVars) companion.sendFrameWithResult(new CmdGetCustomVars(), defaultGetTimeout);
		return customVars;
	}

	private TuningParams tuningParams = null;

	public TuningParams getTuningParams() throws IOException, TimeoutException, InterruptedException {
		if (tuningParams != null)
			return tuningParams;

		tuningParams = (TuningParams) companion.sendFrameWithResult(new CmdGetTuningParams(), defaultGetTimeout);
		return tuningParams;
	}

	public void importContact(byte[] advertPacket) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdImportContact(advertPacket), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	public byte[] exportContact(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdExportContact(pubkey), defaultGetTimeout);
		if (resp instanceof ExportContact)
			return ((ExportContact) resp).getAdvertPacket();
		else
			throw new CompanionErrorException(resp.toString());

	}

	/**
	 * Remove contact from device and main contacts, keep it only in archived
	 * contacts
	 * 
	 * @param pubkey 32B
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void removeContact(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdRemoveContact(pubkey), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		String key = MeshcoreUtils.hex(pubkey);
		Contact c = contacts.remove(key);
		if (c != null) {
			contactsArchive.put(key, c);
		}
	}

	// resetPath // refetch contact after for sync
	public void resetPath(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdResetPath(pubkey), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		refetchContact(pubkey);
	}

	// setAdvertLatLon // refetch after
	public void setAdvertLatLon(double lat, double lon, Integer alt)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetAdvertLatLon(lat, lon, alt), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshSelfInfo();
	}

	/**
	 * 
	 * @param flags   configured with {@link AutoAddConfigFlags}. Working only if
	 *                manualAddContacts is set in SelfInfo (by setOtherParams)
	 * @param maxHops -1 for unlimited
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setAutoAddConfig(byte flags, int maxHops) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetAutoaddConfig(flags, maxHops), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		autoaddConfig = null;
	}

	public void setCustomVar(String name, String value) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetCustomVar(name, value), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		customVars = null;
	}

	// setDevicePin // refetch after
	public void setDevicePIN(Long pin) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetDevicePin(pin == null ? 0l : pin),
				defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshDeviceInfo();
	}

	// setFloodScope // TODO as it isn't final in firmware

	/**
	 * <code>null</code> to keep current value
	 * 
	 * @param manualAddContacts
	 * @param telemetryBaseAll
	 * @param telemetryBaseFav
	 * @param telemetryLocAll
	 * @param telemetryLocFav
	 * @param telemetryEnvAll
	 * @param telemetryEnvFav
	 * @param advertLocPolicy
	 * @param multiAcks
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setOtherParams(Boolean manualAddContacts, Boolean telemetryBaseAll, Boolean telemetryBaseFav,
			Boolean telemetryLocAll, Boolean telemetryLocFav, Boolean telemetryEnvAll, Boolean telemetryEnvFav,
			AdvertLocPolicy advertLocPolicy, Boolean multiAcks)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetOtherParams(
				manualAddContacts == null ? getSelfInfo().isManualAddContacts() : manualAddContacts,
				telemetryBaseAll == null ? getSelfInfo().isTelemetryModeBaseEn() : telemetryBaseAll,
				telemetryBaseFav == null ? getSelfInfo().isTelemetryModeBaseFav() : telemetryBaseFav,
				telemetryLocAll == null ? getSelfInfo().isTelemetryModeLocEn() : telemetryLocAll,
				telemetryLocFav == null ? getSelfInfo().isTelemetryModeLocFav() : telemetryLocFav,
				telemetryEnvAll == null ? getSelfInfo().isTelemetryModeEnvEn() : telemetryEnvAll,
				telemetryEnvFav == null ? getSelfInfo().isTelemetryModeEnvFav() : telemetryEnvFav,
				advertLocPolicy == null ? getSelfInfo().getAdvertLocPolicy() : advertLocPolicy,
				multiAcks == null ? getSelfInfo().getMultiAcks() : multiAcks), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshSelfInfo();
	}

	/**
	 * 
	 * @param mode 0-2 for 1-3B hashes
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setPathHashMode(byte mode) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetPathHashMode(mode), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshDeviceInfo();
	}

	/**
	 * 
	 * @param freq   frequency (kHz)
	 * @param bw     bandwidth (Hz)
	 * @param sf
	 * @param cr
	 * @param repeat act as repeater
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setRadioParams(long freq, long bw, byte sf, byte cr, boolean repeat)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetRadioParams(freq, bw, sf, cr, repeat),
				defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshDeviceInfo();
	}

	/**
	 * 
	 * @param power (Dbm), -9 - SelfInfo.maxLoraPowerDbm
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setRadioTxPower(byte power) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetRadioTXPower(power), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshSelfInfo();
	}

	/**
	 *
	 * @param rxDelayBase
	 * @param airtimeFactor
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void setTuningParams(double rxDelayBase, double airtimeFactor)
			throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetTuningParams(rxDelayBase, airtimeFactor),
				defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		tuningParams = null;
	}

	public BattAndStorage getBattAndStorage() throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdGetBattAndStorage(), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (BattAndStorage) resp;
	}

	public CurrTime getDeviceTime() throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdGetDeviceTime(), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (CurrTime) resp;
	}

	/**
	 * Set device time. Firmware rejects timestamps older than the current device
	 * time.
	 *
	 * @param timestamp Unix epoch seconds, or {@code null} to use current system
	 *                  time
	 */
	public void setDeviceTime(Long timestamp) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetDeviceTime(timestamp), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	public void setAdvertName(String name) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetAdvertName(name), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		companion.refreshSelfInfo();
	}

	/**
	 * Set flood scope transport key. Pass {@code null} to clear (global scope).
	 */
	public void setFloodScope(byte[] scope) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdSetFloodScope(scope), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	public Stats getStats(StatsCommandFrameSubtype subtype) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdGetStats(subtype), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (Stats) resp;
	}

	/**
	 * Add or update a saved contact on the device.
	 */
	public void addUpdateContact(Contact contact) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(contact.getCmdAddUpdateContact(), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/**
	 * Broadcast the contact's advert packet zero-hop so nearby nodes learn about
	 * them.
	 *
	 * @param pubkey 32-byte public key of the contact to share
	 */
	public void shareContact(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdShareContact(pubkey), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
	}

	/**
	 * Fetch the stored outbound advert path for a contact.
	 *
	 * @param pubkey 32-byte public key
	 */
	public AdvertPath getAdvertPath(byte[] pubkey) throws IOException, TimeoutException, InterruptedException {
		ResponseFrame resp = companion.sendFrameWithResult(new CmdGetAdvertPath(pubkey), defaultGetTimeout);
		if (resp instanceof Error)
			throw new CompanionErrorException(resp.toString());
		return (AdvertPath) resp;
	}
}
