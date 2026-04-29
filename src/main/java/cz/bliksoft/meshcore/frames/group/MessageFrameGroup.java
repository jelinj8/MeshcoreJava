package cz.bliksoft.meshcore.frames.group;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.push.LogRXDataPush;

/**
 * Base class for response frames that represent received messages.
 *
 * <p>
 * A message frame may optionally be paired with a {@link LogRXDataPush} that
 * carries the corresponding raw RF metadata (SNR, RSSI, OTA packet) for the
 * same transmission.
 * </p>
 */
public abstract class MessageFrameGroup extends ResponseFrame {

	public MessageFrameGroup(MeshcoreCompanion source, byte[] data) {
		super(source, data);
	}

	private LogRXDataPush pairedLogFrame;

	/**
	 * Returns the raw RF log frame paired with this message, or {@code null} if
	 * none was associated.
	 *
	 * @return the paired {@link LogRXDataPush}, or {@code null}
	 */
	public LogRXDataPush getPairedLogFrame() {
		return pairedLogFrame;
	}

	/**
	 * Associates a raw RF log frame with this message for RF metadata correlation.
	 *
	 * @param frame the {@link LogRXDataPush} to pair, or {@code null} to clear
	 */
	public void setPairedLogFrame(LogRXDataPush frame) {
		this.pairedLogFrame = frame;
	}

}
