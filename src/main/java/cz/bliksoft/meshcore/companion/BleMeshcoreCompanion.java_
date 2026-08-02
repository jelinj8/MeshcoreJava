package cz.bliksoft.meshcore.companion;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simplejavable.Adapter;
import org.simplejavable.BluetoothUUID;
import org.simplejavable.Peripheral;

import cz.bliksoft.meshcore.frames.FrameConstants;

/**
 * BLE transport for MeshcoreCompanion using the Nordic UART Service (NUS). Uses
 * SimpleBLE (simplejavable) for cross-platform BLE support.
 *
 * Download simplejavable.jar from
 * https://github.com/simpleble/simpleble/releases and place at
 * lib/simplejavable.jar. Also add the platform native lib (libsimplejavable.so
 * / libsimplejavable.dll) to java.library.path.
 *
 * NUS UUIDs are exposed as public constants so callers can override if the
 * firmware uses a different BLE profile.
 */
public class BleMeshcoreCompanion extends MeshcoreCompanion {
	private static final Logger log = Logger.getLogger(BleMeshcoreCompanion.class.getName());

	/**
	 * Nordic UART Service (NUS) — base service UUID; standard serial-over-BLE
	 * profile.
	 */
	public static final BluetoothUUID NUS_SERVICE = new BluetoothUUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	/** NUS TX characteristic — phone writes to device (Write Without Response). */
	public static final BluetoothUUID NUS_TX = new BluetoothUUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
	/** NUS RX characteristic — device sends notifications to the phone. */
	public static final BluetoothUUID NUS_RX = new BluetoothUUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

	/** Scan duration used when connecting (milliseconds). */
	private static final int SCAN_TIMEOUT_MS = 5000;

	private final String deviceAddress;
	private volatile Peripheral peripheral;
	// BLE ATT default: 23 bytes frame - 3 bytes ATT header = 20 bytes payload
	private volatile int blePayloadSize = 20;

	private final LinkedBlockingQueue<byte[]> rxChunks = new LinkedBlockingQueue<>();

	// frame reassembly buffer: max frame = 3 header + 65535 payload
	private final byte[] rxBuffer = new byte[65538];
	private int rxBufLen = 0;

	/**
	 * Creates a companion that connects to a MeshCore radio over BLE and
	 * immediately starts the reader loop.
	 *
	 * @param name          companion name (for logging/identity)
	 * @param deviceAddress BLE MAC address of the MeshCore radio (e.g.
	 *                      "AA:BB:CC:DD:EE:FF")
	 */
	public BleMeshcoreCompanion(String name, String deviceAddress) {
		super(name);
		this.deviceAddress = Objects.requireNonNull(deviceAddress, "deviceAddress");
		startLoop();
	}

	// ─── Transport abstract methods ───────────────────────────────────────────

	@Override
	public boolean isConnected() {
		Peripheral p = peripheral;
		return p != null && p.isConnected();
	}

	@Override
	void checkConnection() throws IOException {
		if (!isConnected())
			throw new IOException("BLE not connected to " + deviceAddress);
	}

	@Override
	protected synchronized void sendBinaryFrame(byte[] payload) throws IOException {
		checkConnection();
		Peripheral p = peripheral;
		int len = payload.length;
		// wire frame: '<' + u16le-length + payload (same as serial)
		byte[] frame = new byte[3 + len];
		frame[0] = (byte) FrameConstants.SERIAL_FRAME_TO_RADIO;
		frame[1] = (byte) (len & 0xFF);
		frame[2] = (byte) ((len >>> 8) & 0xFF);
		System.arraycopy(payload, 0, frame, 3, len);
		// send in MTU-sized chunks; NUS TX uses Write Without Response
		int chunkSize = blePayloadSize;
		try {
			for (int off = 0; off < frame.length; off += chunkSize) {
				int end = Math.min(off + chunkSize, frame.length);
				p.writeCommand(NUS_SERVICE, NUS_TX, Arrays.copyOfRange(frame, off, end));
			}
		} catch (Exception e) {
			throw new IOException("BLE write failed", e);
		}
	}

	@Override
	protected byte[] getBinaryFrame() throws IOException {
		while (true) {
			byte[] chunk;
			try {
				chunk = rxChunks.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			if (chunk == null) {
				if (!isConnected() || terminate)
					return null;
				continue;
			}
			// append chunk to reassembly buffer
			if (rxBufLen + chunk.length > rxBuffer.length) {
				// overflow — discard and resync
				rxBufLen = 0;
			}
			System.arraycopy(chunk, 0, rxBuffer, rxBufLen, chunk.length);
			rxBufLen += chunk.length;

			byte[] frame = tryParseFrame();
			if (frame != null)
				return frame;
		}
	}

	// ─── Connection lifecycle ─────────────────────────────────────────────────

	/** Starts the BLE reader thread with exponential back-off reconnection. */
	protected void startLoop() {
		this.readerThread = new Thread(() -> {
			long backoffMs = 500;
			while (!terminate) {
				try {
					connectBle();
					backoffMs = 500;
					onDeviceConnected();
					runLoop();
					throw new EOFException("BLE disconnected");
				} catch (Exception e) {
					if (terminate)
						return;
					onDeviceDisconnected(e);
					disconnectBle();
					try {
						Thread.sleep(backoffMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return;
					}
					backoffMs = Math.min(10_000, backoffMs * 2);
				}
			}
		}, "MeshcoreBleReader");
		this.readerThread.setDaemon(true);
		this.readerThread.start();
	}

	private void connectBle() throws IOException {
		rxChunks.clear();
		rxBufLen = 0;

		List<Adapter> adapters = Adapter.getAdapters();
		if (adapters.isEmpty())
			throw new IOException("No Bluetooth adapters found");
		Adapter adapter = adapters.get(0);

		try {
			adapter.scanFor(SCAN_TIMEOUT_MS);
		} catch (Exception e) {
			throw new IOException("BLE scan failed", e);
		}

		Peripheral found = null;
		for (Peripheral p : adapter.scanGetResults()) {
			if (deviceAddress.equalsIgnoreCase(p.getAddress().toString())) {
				found = p;
				break;
			}
		}
		if (found == null)
			throw new IOException("BLE device not found during scan: " + deviceAddress);

		try {
			found.connect();
		} catch (Exception e) {
			throw new IOException("BLE connect failed to " + deviceAddress, e);
		}

		if (!found.isPaired()) {
			try {
				found.disconnect();
			} catch (Exception ignore) {
			}
			throw new IOException(
					"BLE device " + deviceAddress + " is not paired — pair it via Windows Bluetooth Settings first");
		}

		try {
			found.notify(NUS_SERVICE, NUS_RX, data -> rxChunks.add(data));
		} catch (Exception e) {
			try {
				found.disconnect();
			} catch (Exception ignore) {
			}
			throw new IOException("Failed to subscribe to NUS notifications", e);
		}

		this.peripheral = found;
	}

	private void disconnectBle() {
		Peripheral p = peripheral;
		peripheral = null;
		if (p != null) {
			try {
				p.disconnect();
			} catch (Exception ignore) {
			}
		}
	}

	@Override
	protected void onDeviceConnected() {
		super.onDeviceConnected();
		log.info(String.format("BLE connected to %s", deviceAddress));
	}

	@Override
	protected void onDeviceDisconnected(Exception cause) {
		super.onDeviceDisconnected(cause);
		if (cause instanceof IOException)
			log.warning(String.format("BLE disconnected from %s: %s", deviceAddress, cause));
		else
			log.log(Level.SEVERE, "BLE communication error", cause);
	}

	@Override
	public void close() {
		super.close();
		disconnectBle();
	}

	// ─── Frame reassembly ─────────────────────────────────────────────────────

	/**
	 * Parse one complete frame payload from rxBuffer. Resyncs on the '>' start
	 * byte, returns payload (without header) on success, null if more data is
	 * needed.
	 */
	private byte[] tryParseFrame() {
		// resync: find '>' start byte
		int start = -1;
		for (int i = 0; i < rxBufLen; i++) {
			if ((rxBuffer[i] & 0xFF) == FrameConstants.SERIAL_FRAME_FROM_RADIO) {
				start = i;
				break;
			}
		}
		if (start < 0) {
			rxBufLen = 0;
			return null;
		}
		if (start > 0) {
			rxBufLen -= start;
			System.arraycopy(rxBuffer, start, rxBuffer, 0, rxBufLen);
		}
		// need at least 3 bytes: '>' + len_lo + len_hi
		if (rxBufLen < 3)
			return null;
		int len = (rxBuffer[1] & 0xFF) | ((rxBuffer[2] & 0xFF) << 8);
		if (rxBufLen < 3 + len)
			return null;
		byte[] payload = new byte[len];
		System.arraycopy(rxBuffer, 3, payload, 0, len);
		int consumed = 3 + len;
		rxBufLen -= consumed;
		System.arraycopy(rxBuffer, consumed, rxBuffer, 0, rxBufLen);
		return payload;
	}

	// ─── Utility: scanning ────────────────────────────────────────────────────

	/**
	 * Scan for BLE devices and return a list of "address (name)" strings for all
	 * visible peripherals. Use this to discover the address to pass to the
	 * constructor.
	 *
	 * @param timeoutMs scan duration in milliseconds
	 * @return list of "address (name)" strings for each discovered peripheral
	 * @throws IOException if no adapter found or scan fails
	 */
	public static List<String> scanForNusDevices(int timeoutMs) throws IOException {
		List<Adapter> adapters = Adapter.getAdapters();
		if (adapters.isEmpty())
			throw new IOException("No Bluetooth adapters found");
		Adapter adapter = adapters.get(0);
		try {
			adapter.scanFor(timeoutMs);
		} catch (Exception e) {
			throw new IOException("BLE scan failed", e);
		}
		List<String> result = new ArrayList<String>();
		for (Peripheral p : adapter.scanGetResults()) {
			result.add(p.getAddress().toString() + " (" + p.getIdentifier() + ")");
		}
		return result;
	}
}
