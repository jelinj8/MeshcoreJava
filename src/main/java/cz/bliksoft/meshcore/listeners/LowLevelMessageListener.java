package cz.bliksoft.meshcore.listeners;

import cz.bliksoft.meshcore.FrameListener;
import cz.bliksoft.meshcore.frames.OtaConstants.OtaPayloadType;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;
import cz.bliksoft.meshcore.otaframe.OtaFrame;
import cz.bliksoft.meshcore.otaframe.OtaGroupFrame;
import cz.bliksoft.meshcore.otaframe.OtaUnicastFrame;

/**
 * Listener for lower level message processing, with transport-level message
 * data available.
 */
public abstract class LowLevelMessageListener implements FrameListener<LogRXDataPush> {

	/**
	 * Set to {@code true} after a successful decryption in the most recent
	 * callback.
	 */
	public boolean contentDecrypted = false;

	@Override
	public void onFrame(LogRXDataPush frame) {
		OtaFrame otaFrame = frame.getOtaFrame();
		if (otaFrame instanceof OtaUnicastFrame) {
			if (otaFrame.payloadType == OtaPayloadType.TXT_MSG) {
				contentDecrypted = frame.tryDecryptTxtMsg(frame.getCompanion().getConfig().getPrivateKey());
				onOtaFrame(otaFrame);
				onOtaUnicastTxtFrame((OtaUnicastFrame) otaFrame);
			} else {
				onOtaFrame(otaFrame);
			}
		} else if (otaFrame instanceof OtaGroupFrame) {
			contentDecrypted = frame.tryDecryptGrpTxt();
			onOtaFrame(otaFrame);
			onOtaGroupFrame((OtaGroupFrame) otaFrame);
		} else {
			onOtaFrame(otaFrame);
		}
	}

	/**
	 * Called for all OTA frame types after the decryption attempt.
	 *
	 * @param otaFrame the parsed OTA frame
	 */
	public void onOtaFrame(OtaFrame otaFrame) {
	};

	/**
	 * Called for unicast text ({@link OtaUnicastFrame}) after a decryption attempt
	 * and after {@link #onOtaFrame(OtaFrame)}.
	 *
	 * @param otaFrame the parsed unicast frame
	 */
	public void onOtaUnicastTxtFrame(OtaUnicastFrame otaFrame) {
	};

	/**
	 * Called for group ({@link OtaGroupFrame}) frames after a decryption attempt
	 * and after {@link #onOtaFrame(OtaFrame)}.
	 *
	 * @param otaFrame the parsed group frame
	 */
	public void onOtaGroupFrame(OtaGroupFrame otaFrame) {
	};

}
