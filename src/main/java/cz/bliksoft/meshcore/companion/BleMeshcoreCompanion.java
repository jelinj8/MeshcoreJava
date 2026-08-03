package cz.bliksoft.meshcore.companion;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.ble.BleAdapter;
import cz.bliksoft.javautils.ble.BleException;
import cz.bliksoft.javautils.ble.BlePeripheral;
import cz.bliksoft.javautils.ble.ScanFilter;

/**
 * BLE transport for MeshcoreCompanion using the Nordic UART Service (NUS). Uses
 * cz.bliksoft.java:common-java-utils-ble (BSToolbox-BLE) for cross-platform BLE support.
 *
 * BSToolbox-BLE drives BLE through a bundled Rust sidecar process rather than an in-process
 * native binding, so a native-side BLE fault (which the previous SimpleBLE/simplejavable-based
 * implementation could hit on connect) surfaces as an IOException here instead of crashing the
 * JVM. Every reconnect attempt starts a fresh sidecar process, so a crashed sidecar is simply
 * retried like any other disconnect.
 *
 * NUS UUIDs are exposed as public constants so callers can override if the firmware uses a
 * different BLE profile. Confirmed against the firmware's own BLE companion protocol docs
 * (meshcore-dev/MeshCore wiki, "Companion Radio Protocol" / BLE section): companion_radio does
 * use the standard NUS UUIDs below, with matching read/write directions to what's implemented
 * here (UUIDs are named from the phone's perspective in this class, from the firmware's
 * perspective in the firmware docs — same characteristics, opposite-facing names).
 *
 * That same source confirms the firmware <b>requires OS-level pairing/bonding with MITM
 * protection</b> (static PIN, typically {@code 123456}) before the connection can be used — this
 * isn't a library limitation, it's how the firmware's GATT security is configured on both ESP32
 * and NRF52 builds. Pair the device via the OS's own Bluetooth settings first; neither this
 * library nor the underlying btleplug sidecar can drive that pairing flow itself.
 *
 * Unlike the USB/serial transport, BLE frames carry <b>no {@code '&lt;'}/{@code '&gt;'} + u16le
 * length header</b> — per the firmware's own docs ("for BLE, a frame is simply a single
 * characteristic value; the BLE link layer already does all the integrity checks"), each GATT
 * write/notification value *is* one complete frame. The original SimpleBLE-based implementation
 * (and this class, until verified against a live device) wrongly reused the serial framing here;
 * {@link #sendBinaryFrame} and {@link #getBinaryFrame} do not add or expect that header.
 */
public class BleMeshcoreCompanion extends MeshcoreCompanion {
	private static final Logger log = Logger.getLogger(BleMeshcoreCompanion.class.getName());

	/** Nordic UART Service (NUS) — base service UUID; standard serial-over-BLE profile. */
	public static final String NUS_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
	/** NUS TX characteristic — phone writes to device (Write Without Response). */
	public static final String NUS_TX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
	/** NUS RX characteristic — device sends notifications to the phone. */
	public static final String NUS_RX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

	private static final String PAIRING_HINT = " — if this device requires bonding, pair it via "
			+ "your OS's Bluetooth settings first (MeshCore's default BLE PIN is 123456)";

	/** Scan duration used when connecting (milliseconds). */
	private static final int SCAN_TIMEOUT_MS = 5000;

	private final String deviceAddress;
	private volatile BleAdapter adapter;
	private volatile BlePeripheral peripheral;

	// Each queued entry is already one complete frame - see the class-level note on BLE framing.
	private final LinkedBlockingQueue<byte[]> rxChunks = new LinkedBlockingQueue<>();

	/**
	 * Creates a companion that connects to a MeshCore radio over BLE and immediately starts the
	 * reader loop.
	 *
	 * @param name          companion name (for logging/identity)
	 * @param deviceAddress BLE MAC address of the MeshCore radio (e.g. "AA:BB:CC:DD:EE:FF")
	 */
	public BleMeshcoreCompanion(String name, String deviceAddress) {
		super(name);
		this.deviceAddress = Objects.requireNonNull(deviceAddress, "deviceAddress");
		startLoop();
	}

	// ─── Transport abstract methods ───────────────────────────────────────────

	@Override
	public boolean isConnected() {
		BlePeripheral p = peripheral;
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
		BlePeripheral p = peripheral;
		// One GATT write == one complete frame (no serial-style header) - see class Javadoc. The
		// TX characteristic only declares WRITE (Write Request), not WRITE_WITHOUT_RESPONSE -
		// verified against a live device - so a plain "write without response" here would be
		// silently dropped by the BLE stack instead of erroring.
		try {
			p.writeCharacteristic(NUS_SERVICE, NUS_TX, payload, true);
		} catch (BleException e) {
			throw new IOException("BLE write failed", e);
		}
	}

	@Override
	protected byte[] getBinaryFrame() throws IOException {
		while (true) {
			byte[] frame;
			try {
				frame = rxChunks.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			if (frame != null)
				return frame;
			if (!isConnected() || terminate)
				return null;
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

		// Every reconnect attempt gets a fresh sidecar process, so a crashed sidecar from a
		// previous attempt is simply retried like any other disconnect — no restart bookkeeping
		// needed here.
		BleAdapter newAdapter;
		try {
			newAdapter = new BleAdapter();
		} catch (BleException e) {
			throw new IOException("Failed to start BLE sidecar", e);
		}
		this.adapter = newAdapter;

		AtomicBoolean found = new AtomicBoolean(false);
		try {
			newAdapter.scan(new ScanFilter(), SCAN_TIMEOUT_MS, (address, name, rssi) -> {
				if (deviceAddress.equalsIgnoreCase(address))
					found.set(true);
			});
		} catch (BleException e) {
			throw new IOException("BLE scan failed", e);
		}
		if (!found.get())
			throw new IOException("BLE device not found during scan: " + deviceAddress);

		BlePeripheral p = newAdapter.getPeripheral(deviceAddress);
		p.setDisconnectListener(reason -> log.info(String.format("BLE %s reported disconnect from %s: %s",
				"sidecar_crashed".equals(reason) ? "sidecar" : "peripheral", deviceAddress, reason)));

		try {
			p.connect();
		} catch (BleException e) {
			throw new IOException("BLE connect failed to " + deviceAddress + PAIRING_HINT, e);
		}

		try {
			p.subscribe(NUS_SERVICE, NUS_RX, (charUuid, data) -> rxChunks.add(data));
		} catch (BleException e) {
			try {
				p.disconnect();
			} catch (BleException ignore) {
			}
			throw new IOException("Failed to subscribe to NUS notifications" + PAIRING_HINT, e);
		}

		this.peripheral = p;
	}

	private void disconnectBle() {
		BlePeripheral p = peripheral;
		BleAdapter a = adapter;
		peripheral = null;
		adapter = null;
		if (p != null) {
			try {
				p.disconnect();
			} catch (Exception ignore) {
			}
		}
		if (a != null) {
			a.close();
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

	// ─── Utility: scanning ────────────────────────────────────────────────────

	/**
	 * Scan for BLE devices and return a list of "address (name)" strings for all visible
	 * peripherals. Use this to discover the address to pass to the constructor.
	 *
	 * @param timeoutMs scan duration in milliseconds
	 * @return list of "address (name)" strings for each discovered peripheral
	 * @throws IOException if no adapter found or scan fails
	 */
	public static List<String> scanForNusDevices(int timeoutMs) throws IOException {
		try (BleAdapter adapter = new BleAdapter()) {
			// A device readvertises repeatedly during the scan window, so the same address shows
			// up in many events - keyed map dedupes by address, upgrading a null/blank name to a
			// real one if a later advertisement carries it, without ever downgrading back.
			Map<String, String> devices = new LinkedHashMap<>();
			adapter.scan(new ScanFilter(), timeoutMs, (address, name, rssi) -> {
				if ((name != null && !name.trim().isEmpty()) || !devices.containsKey(address)) {
					devices.put(address, name);
				}
			});
			List<String> result = new ArrayList<>();
			for (Map.Entry<String, String> e : devices.entrySet()) {
				result.add(e.getKey() + " (" + (e.getValue() != null ? e.getValue() : "") + ")");
			}
			return result;
		} catch (BleException e) {
			throw new IOException("BLE scan failed", e);
		}
	}
}
