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
	 * indicates decryption success
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
	 * called for all types of {@link OtaFrame} frames after decryption attempt
	 * 
	 * @param otaFrame
	 */
	public void onOtaFrame(OtaFrame otaFrame) {
	};

	/**
	 * called for text {@link OtaUnicastFrame} after an attempt to decrypt and after
	 * {@link #onOtaFrame(OtaFrame)}
	 * 
	 * @param otaFrame
	 */
	public void onOtaUnicastTxtFrame(OtaUnicastFrame otaFrame) {
	};

	/**
	 * called for {@link OtaGroupFrame} after an attempt to decrypt and after
	 * {@link #onOtaFrame(OtaFrame)}
	 * 
	 * @param otaFrame
	 */
	public void onOtaGroupFrame(OtaGroupFrame otaFrame) {
	};

}
