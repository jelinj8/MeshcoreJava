This is an implemetation of Meshcore Companion serial interface (https://github.com/meshcore-dev/MeshCore/tree/main/examples/companion_radio). It allows full control of the companion (all communication frame types are implemented) currently via USB.
Firmware compatibility v1.14 (including multibyte routing).

Current version is written by me (reverse-engeneering of the C++ firmware source) with some help from AI (mainly sanity checking, decryption and cleanup).

Everything should be compatible with JDK1.8. It contradicts the "think embedded" suggestion at Meshcore's GitHub as it is a higher level language and the main idea is to keep it maintainable and in sync with the firmware (that is why there is the Frame type hierarchy).

Currently the only dependency is com.fazecast jSerialComm for USB virtual COM and Log4J. When BLE implementation is added, I plan to keep the serial interface and the BLE implementation as separate dependencies (to add just what is needed, there will be probably also a separate BLE version for Windows and Linux as there is no multiplatform Java BLE library).
That should enable this to be used even in Android apps (to finally make an open-source companion app). I'm not an Android developer, I'm going to make a PC (JavaFX based) companion eventually + use this as an API to integrate the companion in a server APP.

BLE implementation requires just implementing a connection class as the frame protocol is common, but it requires finding a Java library to do that and I didn't have an opportunity and motivation for that yet. (See SerialMeshcoreCompanion for what is needed).

All communication is logged (Log4J2 to cz.bliksoft.meshcore.companion.MeshcoreCompanion.DEV FINE).

More detailed logging of air frames can be enabled by setting

```java
// default, just read RSSI, SNR, routing and message types.
LogRXDataPush.isDecodeRaw(true);

// parse also transmitted frames content - that provides also routing hops and deciphering of Group and Unicast text frames where possible (using configured channels for Groups and own key for unicast messages where the recipient is the companion).
LogRXDataPush.isDecodePaylodad(true);

// detailed logging of path hops with contacts identified by prefixes, with multiple values where not unique.
LogRXDataPush.setTranslatePath(true);
```

This is a simple usage example:

```java
public class SimpleMeshcoreCompanion extends SerialMeshcoreCompanion {
	Logger log = LogManager.getLogger();

	public BSAppMeshcore(FileObject definition) throws IOException {
		super(definition.getAttribute("name", definition.getName()),
				Objects.requireNonNull(definition.getAttribute("com"), "com"), definition.getInt("baud", 115200));

		drainMessages();
		installAutosyncMessages();

		FrameListener<ResponseFrame> msgReader = new FrameListener<>() {
			@Override
			public void onFrame(ResponseFrame frame) {
				switch (frame.getFrameType()) {
				case RESP_CHANNEL_MSG_RECV:
				case RESP_CHANNEL_MSG_RECV_V3: {
					ChannelMsgRecv m = (ChannelMsgRecv) frame;
					log.info(String.format("Group message %s: %s", getChannel(m.getChannelIdx()).getName(),
							m.getText()));
				}
					break;
				case RESP_CONTACT_MSG_RECV:
				case RESP_CONTACT_MSG_RECV_V3: {
					ContactMsgRecv m = (ContactMsgRecv) frame;
					log.info("Message: " + m.getText());
					try {
						sendFrameWithResultAndResponse(
								new CmdSendTxtMsg(m.getTextType(), m.getFrom6(), 0, null, "ECHO: " + m.getText()),

								1000l, 1000l);
						log.info("Message confirmed.");
					} catch (IOException e) {
						e.printStackTrace();
					} catch (TimeoutException e) {
						log.info("Message NOT confirmed in time.");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
					break;

				default:
					break;
				}
			}
		};

		registerFrameListener(MessageFrameGroup.class, msgReader);
	}

	@Override
	protected void deviceInit() throws IOException {
		super.deviceInit();
		try {
			sendFrameWithResult(new CmdGetDeviceTime(), 1000l);
			sendFrameWithResult(new CmdSetDeviceTime(java.time.Instant.now().getEpochSecond()), 1000l);
			sendFrameWithResult(new CmdGetAutoaddConfig(), 1000);
			drainMessages();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
```

A BruteForceDecryptor can be registered as a listener to discover and decrypt hash channels:

```java
		GroupTextDecryptor decryptor = new GroupTextDecryptor(1, 6);

		decryptor.install(mesh, new GroupTextDecryptor.Listener() {
			public void onDecrypted(String ch, byte[] key, long ts, String text, float snr, int rssi) {
				log.info(String.format("[%s] %s  (snr=%.1f rssi=%d)%n", ch, text, snr, rssi));
			}

			public void onChannelDiscovered(String ch, byte[] key) {
				log.info(String.format("Discovered channel: %s%n", ch));
			}

			@Override
			public void onBruteForceComplete(byte channelHash, long matched, boolean stopped) {
				log.info(String.format("BruteForce finished (matched %d)", matched));
			}
		});
```

Maximum reasonable length to decrypt is 7 characters (~2 minutes on a 24C CPU).

Contacts and channels are synchronized on connect and then kept synchronized. All communication with the Companion triggers "events" that can have registered "listeners" (by frame class or superclass like MessageFrameGroup or ContactFrameGroup).

NewAdvertPush event adds the contact only in local memory, so it can be later/in a listener added to Companion's storage (.getCmdAddUpdateContact() and send the resulting frame to companion) or just used to identify a node in air traffic.

Suggestions are welcome, more examples might come.
