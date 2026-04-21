package cz.bliksoft.meshcore.bot;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.CompanionErrorException;
import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.companion.MeshcoreCompanionConfig;
import cz.bliksoft.meshcore.companion.SerialMeshcoreCompanion;
import cz.bliksoft.meshcore.frames.FrameConstants.MessageTextType;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdGetDeviceTime;
import cz.bliksoft.meshcore.frames.cmd.CmdSetDeviceTime;
import cz.bliksoft.meshcore.frames.group.MessageFrameGroup;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.frames.push.SendConfirmedPush;
import cz.bliksoft.meshcore.frames.resp.ChannelMsgRecv;
import cz.bliksoft.meshcore.frames.resp.ContactMsgRecv;
import cz.bliksoft.meshcore.listeners.LowLevelMessageListener;
import cz.bliksoft.meshcore.otaframe.OtaGroupFrame;
import cz.bliksoft.meshcore.otaframe.OtaUnicastFrame;
import cz.bliksoft.meshcore.utils.MeshcoreUtils;

public class BotCompanion extends SerialMeshcoreCompanion {

	Logger log = Logger.getLogger(BotCompanion.class.getName());

	public BotCompanion(String name, String portName, int baud) throws IOException {
		super(name, portName, baud);

		installAutosyncMessages();
		LogRXDataPush.setDecodePaylodad(true);
		// LogRXDataPush.setTranslatePath(true);

//		LowLevelMessageListener botListener = new LowLevelMessageListener() {
//			@Override
//			public void onOtaUnicastTxtFrame(OtaUnicastFrame otaFrame) {
//				if (contentDecrypted) {
//					Runnable r = new Runnable() {
//
//						@Override
//						public void run() {
//							log.info(String.format("DETAILED %s: %s", otaFrame.decryptedSender.getName(),
//									otaFrame.decryptedText));
//							try {
//								log.info("Sending trace:\n" + otaFrame.getDetailedPath());
//								sendTxtMsgAsync(MessageTextType.TXT_TYPE_PLAIN, MeshcoreUtils.prefix6(otaFrame.decryptedSender.getPubkey()), 0, null, String.format("PATH:\n%s", otaFrame.getDetailedPath()));
////								sendTxtMsg(MessageTextType.TXT_TYPE_PLAIN,
////										MeshcoreUtils.prefix6(otaFrame.decryptedSender.getPubkey()), 0, null,
////										String.format("PATH:\n%s", otaFrame.getDetailedPath()));
////								Thread.sleep(1000l);
//								log.info("Sending second trace:\n" + otaFrame.getDetailedPath());
//								sendTxtMsgAsync(MessageTextType.TXT_TYPE_PLAIN, MeshcoreUtils.prefix6(otaFrame.decryptedSender.getPubkey()), 0, null, String.format("PATH2:\n%s", otaFrame.getDetailedPath()));
////								sendTxtMsg(MessageTextType.TXT_TYPE_PLAIN,
////										MeshcoreUtils.prefix6(otaFrame.decryptedSender.getPubkey()), 0, null,
////										String.format("PATH2:\n%s", otaFrame.getDetailedPath()));
//							} catch (IOException | TimeoutException | InterruptedException e) {
//								e.printStackTrace();
//							}
//						}
//					};
//
//					Thread t = new Thread(r);
//					r.run();
//				}
//			}
//
//			@Override
//			public void onOtaGroupFrame(OtaGroupFrame otaFrame) {
////				if (contentDecrypted)
////					log.info(String.format("DETAILED GRP %s: %s", otaFrame.decryptedChannelName,
////							otaFrame.decryptedText));
//			}
//
//			@Override
//			public String toString() {
//				return "BSApp diagnostic message listener";
//			}
//		};
//
//		registerFrameListener(LogRXDataPush.class, botListener);

		FrameListener<ResponseFrame> msgReader = new FrameListener<ResponseFrame>() {
			@Override
			public void onFrame(ResponseFrame frame) {
				Runnable r = new Runnable() {

					@Override
					public void run() {
						switch (frame.getFrameType()) {
						case RESP_CHANNEL_MSG_RECV:
						case RESP_CHANNEL_MSG_RECV_V3: {
//							ChannelMsgRecv m = (ChannelMsgRecv) frame;
//							log.info(String.format("Group message %s: %s", getConfig().getChannel(m.getChannelIdx()).getName(),
//									m.getText()));
						}
							break;
						case RESP_CONTACT_MSG_RECV:
						case RESP_CONTACT_MSG_RECV_V3: {
							ContactMsgRecv m = (ContactMsgRecv) frame;
							try {
								Thread.sleep(1000l);
								log.info("sending ECHO: " + m.getText());
								sendTxtMsgAsync(m.getTextType(), m.getFrom6(), 0, null, "ECHO: " + m.getText());
								sendTxtMsgAsync(m.getTextType(), m.getFrom6(), 0, null, "ECHO2: " + m.getText());
								sendTxtMsgAsync(m.getTextType(), m.getFrom6(), 0, null, "ECHO3: " + m.getText());
								// log.info(res.toString());
							} catch (IOException e) {
								e.printStackTrace();
							} catch (TimeoutException e) {
								log.warning("Message NOT confirmed in time.");
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (CompanionErrorException e) {
								log.severe("Meshcore ERR: " + e.getMessage());
							}
						}
							break;

						default:
							break;
						}
					}
				};
				Thread t = new Thread(r);
				t.run();
			}

			@Override
			public String toString() {
				return "BSApp message listener";
			}
		};

		registerFrameListener(MessageFrameGroup.class, msgReader);

		registerFrameListener(SendConfirmedPush.class, new FrameListener<SendConfirmedPush>() {

			@Override
			public void onFrame(SendConfirmedPush frame) {
				log.info(frame.toString());
			}

		});
	}

	@Override
	protected void deviceInit() throws IOException {
		super.deviceInit();

		try {
			sendFrameWithResult(new CmdGetDeviceTime(), 1000l);
			sendFrameWithResult(new CmdSetDeviceTime(java.time.Instant.now().getEpochSecond()), 1000l);
			drainMessages();

//			LogRXDataPush.setDecodeRaw(true);
//			LogRXDataPush.setDecodePaylodad(true);
//			LogRXDataPush.setTranslatePath(true);

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
