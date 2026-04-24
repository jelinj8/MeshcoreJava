package cz.bliksoft.meshcore.companion;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.Frame;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.cmd.CmdAppStart;
import cz.bliksoft.meshcore.frames.cmd.CmdDeviceQuery;
import cz.bliksoft.meshcore.frames.resp.DeviceInfo;
import cz.bliksoft.meshcore.frames.resp.SelfInfo;

/**
 * MeshCore Companion client.
 *
 * Design goals: - Single reader thread owns reading/parsing of frames. -
 * Handlers are dispatched to eventExecutor so they never block reading. -
 * Blocking API is safe to call from any thread EXCEPT the reader thread. -
 * Blocking requests are serialized (requestLock) because the protocol has no
 * request-id.
 */
public abstract class MeshcoreCompanionBase implements Closeable {

	public MeshcoreCompanionBase(String name) {
		this.name = name;
	}

	private static Logger log = Logger.getLogger(MeshcoreCompanionBase.class.getName());
	protected static Logger frameLog = Logger.getLogger(MeshcoreCompanion.class.getName() + ".DEV");

	abstract void checkConnection() throws IOException;

	abstract boolean isConnected();

	protected volatile Thread readerThread = null;

	/**
	 * internal synchronized frame sending, send a whole frame.
	 * 
	 * @param payload
	 * @throws IOException
	 */
	abstract protected void sendBinaryFrame(byte[] payload) throws IOException;

	/**
	 * frame reader Radio -> App. Reader thread only. Expects blocking read of a
	 * complete frame.
	 */
	abstract protected byte[] getBinaryFrame() throws IOException;

	protected final ExecutorService eventExecutor = Executors
			.newSingleThreadExecutor(new NamedDaemonThreadFactory("MeshcoreEvents"));

	private final ReentrantLock requestLock = new ReentrantLock(true);

	/**
	 * termination flag, set by close(), allows exiting of loops.
	 */
	protected volatile boolean terminate = false;

	/**
	 * indicator of running reader loop
	 */
	protected volatile AtomicBoolean running = new AtomicBoolean();

	/**
	 * indicator of a device being ready to process commands
	 */
	protected volatile AtomicBoolean available = new AtomicBoolean();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected String name = "meshcore";

	// single pending blocking request (serialized by requestLock)
	private volatile CompletableFuture<ResponseFrame> pendingFuture;
	private volatile byte[] pendingAcceptCodes;

	// ----------------- Core concurrency: send + await response -----------------

	/**
	 * non blocking call (does not wait for response)
	 * 
	 * @param payload
	 * @throws IOException
	 */
	public void sendFrame(Frame payload) throws IOException {
		requestLock.lock();
		if (frameLog.isLoggable(Level.FINE))
			frameLog.fine(payload.toString());
		try {
			sendBinaryFrame(payload.getBytes());
		} finally {
			requestLock.unlock();
		}
	}

	/**
	 * blocking call, list of expected response types from CommandFrame definition.
	 * 
	 * @param payload
	 * @param timeoutMs
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public ResponseFrame sendFrameWithResult(CommandFrame payload, long timeoutMs)
			throws IOException, TimeoutException, InterruptedException {
		return sendFrameWithResult(payload, timeoutMs, payload.expectedResponses());
	}

	/**
	 * blocking call, waits for specified response frame types.
	 * 
	 * @param payload
	 * @param timeoutMs
	 * @param acceptCodes
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public ResponseFrame sendFrameWithResult(CommandFrame payload, long timeoutMs, byte... acceptCodes)
			throws IOException, TimeoutException, InterruptedException {
		checkBlockingThread();
		requestLock.lock();
		if (frameLog.isLoggable(Level.FINE))
			frameLog.fine(payload.toString());
		try {
			CompletableFuture<ResponseFrame> f = new CompletableFuture<>();
			pendingFuture = f;
			if (acceptCodes == null || acceptCodes.length == 0)
				throw new IllegalArgumentException(
						"Frame does not specify any expected ResponseFrameTypes, can't wait for response. Use sendFrame.");

			pendingAcceptCodes = acceptCodes;
			sendBinaryFrame(payload.getBytes());
			return getFuture(f, timeoutMs);
		} finally {
			pendingFuture = null;
			pendingAcceptCodes = null;
			requestLock.unlock();
		}
	}

	protected static ResponseFrame getFuture(CompletableFuture<ResponseFrame> f, long timeoutMs)
			throws TimeoutException, InterruptedException, IOException {
		try {
			return f.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			throw te;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw ie;
		} catch (ExecutionException ee) {
			Throwable c = ee.getCause();
			if (c instanceof IOException)
				throw (IOException) c;
			throw new IOException("Request failed", c);
		}
	}

	/**
	 * Wait for event loop to start up, mainly for internal use.
	 * 
	 * @param timeoutMs
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	protected void awaitRunning(long timeoutMs) throws TimeoutException, InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (running.get()) {
				return;
			}
			Thread.sleep(50);
		}
		throw new TimeoutException("Meshcore loop not running in time");
	}

	/**
	 * Wait for event loop to start up and finish initialization (after each
	 * reconnect). This is the way to check for device OK on startup.
	 * 
	 * @param timeoutMs
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public void awaitAvailable(long timeoutMs) throws TimeoutException, InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (available.get()) {
				return;
			}
			Thread.sleep(50);
		}
		throw new TimeoutException("Meshcore device not available in time");
	}

	/**
	 * check if the Meshcore companion device is connected and available to accept
	 * commands (initialized)
	 * 
	 * @return
	 */
	public boolean isAvailable() {
		return available.get();
	}

	/**
	 * Main Meshcore input point. Keep running in its own thread, do not block by
	 * waiting for events (they will not trigger if blocked!)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	protected void runLoop() throws IOException, TimeoutException, InterruptedException {
		eventExecutor.execute(() -> {
			try {
				running.set(true);
				deviceHandshake();
			} catch (IOException | TimeoutException | InterruptedException e) {
				running.set(false);
				log.severe(String.format("Failed to initialize Mesh companion device: %s", e));
			}
		});
		try {
			// wait for processing of device handshake
			awaitRunning(2000l);
			eventExecutor.execute(() -> {
				try {
					deviceInit();
					available.set(true);
				} catch (IOException e) {
					available.set(false);
					log.severe(String.format("Failed to initialize Mesh companion device: %s", e));
				}
			});
			while (!terminate && isConnected()) {
				byte[] p = getBinaryFrame();
				if (p == null)
					return;
				dispatchFrame(p);
			}
		} catch (TimeoutException to) {
			log.severe("Mesh device not initialized in time.");
		} finally {
			running.set(false);
		}
	}

	private DeviceInfo deviceInfo = null;
	private SelfInfo selfInfo = null;

	public DeviceInfo getDeviceInfo() {
		return deviceInfo;
	}

	public SelfInfo getSelfInfo() {
		return selfInfo;
	}

	public void refreshDeviceInfo() throws IOException, TimeoutException, InterruptedException {
		deviceInfo = (DeviceInfo) sendFrameWithResult(new CmdDeviceQuery(), 1000);
	}

	public void refreshSelfInfo() throws IOException, TimeoutException, InterruptedException {
		selfInfo = (SelfInfo) sendFrameWithResult(new CmdAppStart("BSMeshcore"), 1000);
	}

	/**
	 * initial protocol handshake, loop not yet running
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	protected void deviceHandshake() throws IOException, TimeoutException, InterruptedException {
		refreshDeviceInfo();
		refreshSelfInfo();
	}

	/**
	 * additional initialization in online mode (e.g. set time), this is the place
	 * to initialize after connection.
	 * 
	 * @throws IOException
	 */
	protected void deviceInit() throws IOException {

	}

	/**
	 * call when a source device is available
	 */
	protected void onDeviceConnected() {
	}

	/**
	 * call when source device becomes unavailable/disconnected. Be sure to call
	 * this base as it does cleanup!
	 * 
	 * @param cause
	 */
	protected void onDeviceDisconnected(Exception cause) {
		failAllWaiters(new IOException("Disconnected", cause));
	}

	// ----------------- Utils -----------------

	protected static final class NamedDaemonThreadFactory implements ThreadFactory {
		private final String baseName;
		private int idx = 1;

		NamedDaemonThreadFactory(String baseName) {
			this.baseName = baseName;
		}

		@Override
		public synchronized Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName(baseName + "-" + (idx++));
			return t;
		}
	}

	/** Close and stop background threads. */
	public void close() {
		log.info("Meshcore close");
		terminate = true;
		// fail pending request
		CompletableFuture<ResponseFrame> f = pendingFuture;
		if (f != null && !f.isDone()) {
			f.completeExceptionally(new IOException("Closed"));
		}
		pendingFuture = null;
		pendingAcceptCodes = null;

		// stop events
		eventExecutor.shutdownNow();
	}

	protected void failAllWaiters(IOException ex) {
		// pending request/response
		CompletableFuture<ResponseFrame> pf = pendingFuture;
		if (pf != null)
			pf.completeExceptionally(ex);
		pendingFuture = null;
		pendingAcceptCodes = null;

	}

	protected void checkBlockingThread() {
		if (readerThread == null) {
			throw new IllegalStateException("readerThread not set!");
		}
		if (!readerThread.isAlive() || !running.get()) {
			throw new IllegalStateException("Current readerThread or reader loop is not running!");
		}
		if (Thread.currentThread() == readerThread) {
			throw new IllegalStateException(
					"Blocking API cannot be called from reader thread. Dispatch work to eventExecutor.");
		}
	}

	/**
	 * process data frame received from device
	 * 
	 * @param data
	 * @throws IOException
	 */
	protected void dispatchFrame(byte[] data) throws IOException {
		ResponseFrame f = (ResponseFrame) Frame.fromData((MeshcoreCompanion) this, data);
		dispatchFrame(f);
	}

	/**
	 * Process received frame.
	 * 
	 * @param frame
	 * @return true if processed
	 * @throws IOException
	 */
	protected boolean dispatchFrame(ResponseFrame frame) throws IOException {
		if (frame == null)
			return true;

		if (frameLog.isLoggable(Level.FINE)) {
			try {
				frameLog.fine(frame.toString());
			} catch (Exception e) {
				frameLog.log(Level.SEVERE, String.format("Failed to log frame %s", frame.getFrameType()), e);
			}
		}

		// 1) complete pending blocking request if it matches
		CompletableFuture<ResponseFrame> f = pendingFuture;
		byte[] accept = pendingAcceptCodes;
		if (f != null && accept != null && accept.length > 0) {
			for (byte ac : accept) {
				if (frame.is(ac)) {
					f.complete(frame);
					return true; // consumed by request
				}
			}
		}

		return false;
	}
}
