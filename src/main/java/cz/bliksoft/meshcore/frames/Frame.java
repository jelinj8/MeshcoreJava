package cz.bliksoft.meshcore.frames;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.push.AdvertPush;
import cz.bliksoft.meshcore.frames.push.BinaryResponsePush;
import cz.bliksoft.meshcore.frames.push.ContactDeletedPush;
import cz.bliksoft.meshcore.frames.push.ContactsFullPush;
import cz.bliksoft.meshcore.frames.push.ControlDataPush;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.frames.push.LoginFailPush;
import cz.bliksoft.meshcore.frames.push.LoginSuccessPush;
import cz.bliksoft.meshcore.frames.push.MessageWaitingPush;
import cz.bliksoft.meshcore.frames.push.NewAdvertPush;
import cz.bliksoft.meshcore.frames.push.PathDiscoveryResponsePush;
import cz.bliksoft.meshcore.frames.push.PathUpdatedPush;
import cz.bliksoft.meshcore.frames.push.RawDataPush;
import cz.bliksoft.meshcore.frames.push.SendConfirmedPush;
import cz.bliksoft.meshcore.frames.push.StatusResponsePush;
import cz.bliksoft.meshcore.frames.push.TelemetryResponsePush;
import cz.bliksoft.meshcore.frames.push.TraceDataPush;
import cz.bliksoft.meshcore.frames.resp.AdvertPath;
import cz.bliksoft.meshcore.frames.resp.AllowedRepeatFreq;
import cz.bliksoft.meshcore.frames.resp.AutoaddConfig;
import cz.bliksoft.meshcore.frames.resp.BattAndStorage;
import cz.bliksoft.meshcore.frames.resp.ChannelDataRecv;
import cz.bliksoft.meshcore.frames.resp.ChannelInfo;
import cz.bliksoft.meshcore.frames.resp.ChannelMsgRecv;
import cz.bliksoft.meshcore.frames.resp.DefaultFloodScope;
import cz.bliksoft.meshcore.frames.resp.Contact;
import cz.bliksoft.meshcore.frames.resp.ContactMsgRecv;
import cz.bliksoft.meshcore.frames.resp.ContactsStart;
import cz.bliksoft.meshcore.frames.resp.CurrTime;
import cz.bliksoft.meshcore.frames.resp.CustomVars;
import cz.bliksoft.meshcore.frames.resp.DeviceInfo;
import cz.bliksoft.meshcore.frames.resp.Disabled;
import cz.bliksoft.meshcore.frames.resp.EndOfContacts;
import cz.bliksoft.meshcore.frames.resp.ExportContact;
import cz.bliksoft.meshcore.frames.resp.NoMoreMessages;
import cz.bliksoft.meshcore.frames.resp.Ok;
import cz.bliksoft.meshcore.frames.resp.PrivateKey;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;
import cz.bliksoft.meshcore.frames.resp.Sent;
import cz.bliksoft.meshcore.frames.resp.SignStart;
import cz.bliksoft.meshcore.frames.resp.Signature;
import cz.bliksoft.meshcore.frames.resp.Stats;
import cz.bliksoft.meshcore.frames.resp.TuningParams;

public abstract class Frame {

	public enum FrameType {
		COMMAND, PUSH, RESPONSE;
	}

	public abstract byte getTypeCode();

	public abstract boolean is(FrameType type);

	/**
	 * check if the frame is of specified type
	 *
	 * @param type raw type byte to compare against
	 * @return true if this frame's type code equals {@code type}
	 */
	public boolean is(byte type) {
		return getTypeCode() == type;
	}

	/**
	 * Frame factory from binary data
	 *
	 * @param source companion that received the frame
	 * @param data   raw frame bytes (first byte is the type code)
	 * @return typed frame, or {@code null} if data is null or empty
	 */
	public static Frame fromData(MeshcoreCompanion source, byte[] data) {
		if (data == null || data.length < 1)
			return null;

		if ((data[0] & 0xFF) < 128) { // response frame
		}
		switch (ResponseFrameType.fromByte(data[0])) {
		case PUSH_ADVERT:
			return new AdvertPush(source, data);
		case RESP_ADVERT_PATH:
			return new AdvertPath(source, data);
		case RESP_ALLOWED_REPEAT_FREQ:
			return new AllowedRepeatFreq(source, data);
		case RESP_AUTOADD_CONFIG:
			return new AutoaddConfig(source, data);
		case RESP_CHANNEL_DATA_RECV:
			return new ChannelDataRecv(source, data);
		case RESP_DEFAULT_FLOOD_SCOPE:
			return new DefaultFloodScope(source, data);
		case RESP_BATT_AND_STORAGE:
			return new BattAndStorage(source, data);
		case PUSH_BINARY_RESPONSE:
			return new BinaryResponsePush(source, data);
		case RESP_CHANNEL_INFO:
			return new ChannelInfo(source, data);
		case RESP_CHANNEL_MSG_RECV:
			return new ChannelMsgRecv(source, data);
		case RESP_CHANNEL_MSG_RECV_V3:
			return new ChannelMsgRecv(source, data);
		case RESP_CONTACT:
			return new Contact(source, data);
		case PUSH_CONTACTS_FULL:
			return new ContactsFullPush(source, data);
		case RESP_CONTACTS_START:
			return new ContactsStart(source, data);
		case PUSH_CONTACT_DELETED:
			return new ContactDeletedPush(source, data);
		case RESP_CONTACT_MSG_RECV:
			return new ContactMsgRecv(source, data);
		case RESP_CONTACT_MSG_RECV_V3:
			return new ContactMsgRecv(source, data);
		case PUSH_CONTROL_DATA:
			return new ControlDataPush(source, data);
		case RESP_CURR_TIME:
			return new CurrTime(source, data);
		case RESP_CUSTOM_VARS:
			return new CustomVars(source, data);
		case RESP_DEVICE_INFO:
			return new DeviceInfo(source, data);
		case RESP_DISABLED:
			return new Disabled(source, data);
		case RESP_END_OF_CONTACTS:
			return new EndOfContacts(source, data);
		case RESP_ERR:
			return new cz.bliksoft.meshcore.frames.resp.Error(source, data);
		case RESP_EXPORT_CONTACT:
			return new ExportContact(source, data);
		case PUSH_LOGIN_FAIL:
			return new LoginFailPush(source, data);
		case PUSH_LOGIN_SUCCESS:
			return new LoginSuccessPush(source, data);
		case PUSH_LOG_RX_DATA:
			return new LogRXDataPush(source, data);
		case PUSH_MSG_WAITING:
			return new MessageWaitingPush(source, data);
		case PUSH_NEW_ADVERT:
			return new NewAdvertPush(source, data);
		case RESP_NO_MORE_MESSAGES:
			return new NoMoreMessages(source, data);
		case RESP_OK:
			return new Ok(source, data);
		case PUSH_PATH_DISCOVERY_RESPONSE:
			return new PathDiscoveryResponsePush(source, data);
		case PUSH_PATH_UPDATED:
			return new PathUpdatedPush(source, data);
		case RESP_PRIVATE_KEY:
			return new PrivateKey(source, data);
		case PUSH_RAW_DATA:
			return new RawDataPush(source, data);
		case RESP_SELF_INFO:
			return new SelfInfo(source, data);
		case PUSH_SEND_CONFIRMED:
			return new SendConfirmedPush(source, data);
		case RESP_SENT:
			return new Sent(source, data);
		case RESP_SIGNATURE:
			return new Signature(source, data);
		case RESP_SIGN_START:
			return new SignStart(source, data);
		case RESP_STATS:
			return new Stats(source, data);
		case PUSH_STATUS_RESPONSE:
			return new StatusResponsePush(source, data);
		case PUSH_TELEMETRY_RESPONSE:
			return new TelemetryResponsePush(source, data);
		case PUSH_TRACE_DATA:
			return new TraceDataPush(source, data);
		case RESP_TUNING_PARAMS:
			return new TuningParams(source, data);

		default:
			if ((data[0] & 0xFF) < 128) // response frame
				return new UnknownResponseFrame(source, data);
			else
				return new UnknownPushFrame(source, data);
		}
	}

	/**
	 * Serialize to the binary wire representation.
	 *
	 * @return raw frame bytes ready to send or store
	 */
	public abstract byte[] getBytes();
}
