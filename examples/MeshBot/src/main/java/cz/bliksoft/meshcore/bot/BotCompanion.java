package cz.bliksoft.meshcore.bot;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.companion.SerialMeshcoreCompanion;
import cz.bliksoft.meshcore.frames.cmd.CmdGetDeviceTime;
import cz.bliksoft.meshcore.frames.cmd.CmdSetDeviceTime;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.listeners.LowLevelMessageListener;
import cz.bliksoft.meshcore.otaframe.OtaGroupFrame;
import cz.bliksoft.meshcore.otaframe.OtaUnicastFrame;

public class BotCompanion extends SerialMeshcoreCompanion {

	Logger log = Logger.getLogger(BotCompanion.class.getName());

	public BotCompanion(String name, String portName, int baud) throws IOException {
		super(name, portName, baud);

		installAutosyncMessages();

		LowLevelMessageListener botListener = new LowLevelMessageListener() {
			@Override
			public void onOtaUnicastTxtFrame(OtaUnicastFrame otaFrame) {
				if (contentDecrypted)
					log.info(String.format("DETAILED %s: %s", otaFrame.decryptedSender.getName(),
							otaFrame.decryptedText));
			}

			@Override
			public void onOtaGroupFrame(OtaGroupFrame otaFrame) {
				if (contentDecrypted)
					log.info(String.format("DETAILED GRP %s: %s", otaFrame.decryptedChannelName,
							otaFrame.decryptedText));
			}

			@Override
			public String toString() {
				return "BSApp diagnostic message listener";
			}
		};

		registerFrameListener(LogRXDataPush.class, botListener);
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
