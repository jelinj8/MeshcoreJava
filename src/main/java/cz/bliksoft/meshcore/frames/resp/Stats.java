package cz.bliksoft.meshcore.frames.resp;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.StatsCommandFrameSubtype;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetStats} exposing one
 * of three statistics subtypes: core system metrics (battery, uptime, queue),
 * radio signal metrics, or packet counters.
 */
public class Stats extends ResponseFrame {

	/**
	 * @return the statistics subtype reported in this frame
	 */
	public StatsCommandFrameSubtype getSubtype() {
		return subtype;
	}

	/**
	 * @return battery voltage in millivolts; {@code -1} if not available (subtype
	 *         != CORE)
	 */
	public int getBattmV() {
		return battmV;
	}

	/**
	 * @return device uptime in seconds; {@code -1} if not available (subtype !=
	 *         CORE)
	 */
	public long getUptimeS() {
		return uptimeS;
	}

	/**
	 * @return bitmask of error flags; {@code 0} if not available (subtype != CORE)
	 */
	public int getErrflags() {
		return errflags;
	}

	/**
	 * @return current outgoing packet queue length; {@code -1} if not available
	 *         (subtype != CORE)
	 */
	public int getQueueLen() {
		return queueLen;
	}

	/**
	 * @return LoRa noise floor in dBm; {@code 0} if not available (subtype !=
	 *         RADIO)
	 */
	public int getNoiseFloor() {
		return noiseFloor;
	}

	/**
	 * @return RSSI of the last received packet in dBm; {@code 0} if not available
	 *         (subtype != RADIO)
	 */
	public int getLastRssi() {
		return lastRssi;
	}

	/**
	 * @return SNR of the last received packet multiplied by 4; divide by 4.0 for dB
	 *         value; {@code 0} if not available (subtype != RADIO)
	 */
	public int getLastSnr4() {
		return lastSnr4;
	}

	/**
	 * @return total time spent transmitting on air in seconds; {@code -1} if not
	 *         available (subtype != RADIO)
	 */
	public long getTxAirSec() {
		return txAirSec;
	}

	/**
	 * @return total time spent receiving on air in seconds; {@code -1} if not
	 *         available (subtype != RADIO)
	 */
	public long getRxAirSec() {
		return rxAirSec;
	}

	/**
	 * @return total number of packets received; {@code -1} if not available
	 *         (subtype != PACKETS)
	 */
	public long getRecv() {
		return recv;
	}

	/**
	 * @return total number of packets sent; {@code -1} if not available (subtype !=
	 *         PACKETS)
	 */
	public long getSent() {
		return sent;
	}

	/**
	 * @return number of flood packets sent; {@code -1} if not available (subtype !=
	 *         PACKETS)
	 */
	public long getSentFlood() {
		return sentFlood;
	}

	/**
	 * @return number of direct packets sent; {@code -1} if not available (subtype
	 *         != PACKETS)
	 */
	public long getSentDirect() {
		return sentDirect;
	}

	/**
	 * @return number of flood packets received; {@code -1} if not available
	 *         (subtype != PACKETS)
	 */
	public long getRecvFlood() {
		return recvFlood;
	}

	/**
	 * @return number of direct packets received; {@code -1} if not available
	 *         (subtype != PACKETS)
	 */
	public long getRecvDirect() {
		return recvDirect;
	}

	/**
	 * @return number of receive errors; {@code -1} if not available (subtype !=
	 *         PACKETS)
	 */
	public long getRecvErrors() {
		return recvErrors;
	}

	final StatsCommandFrameSubtype subtype;

	// core
	int battmV = -1;
	long uptimeS = -1;
	int errflags = 0;
	int queueLen = -1;

	// radio
	int noiseFloor = 0;
	int lastRssi = 0;
	int lastSnr4 = 0;
	long txAirSec = -1;
	long rxAirSec = -1;

	// packets
	long recv = -1;
	long sent = -1;
	long sentFlood = -1;
	long sentDirect = -1;
	long recvFlood = -1;
	long recvDirect = -1;
	long recvErrors = -1;

	public Stats(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();
		subtype = StatsCommandFrameSubtype.fromByte(br.readByte());

		switch (subtype) {
		case CORE:
			battmV = br.readUInt16LE();
			uptimeS = br.readUInt32LE();
			errflags = br.readInt16LE();
			queueLen = br.readUnsignedByte();
			break;
		case RADIO:
			noiseFloor = br.readInt16LE();
			lastRssi = br.readByte();
			lastSnr4 = br.readByte();
			txAirSec = br.readUInt32LE();
			rxAirSec = br.readUInt32LE();
			break;
		case PACKETS:
			recv = br.readUInt32LE();
			sent = br.readUInt32LE();
			sentFlood = br.readUInt32LE();
			sentDirect = br.readUInt32LE();
			recvFlood = br.readUInt32LE();
			recvDirect = br.readUInt32LE();
			recvErrors = br.readUInt32LE();
			break;
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_STATS;
	}

	@Override
	public String toString() {
		switch (subtype) {
		case CORE:
			return String.format("RESP_STATS CORE batt %dmV uptime(s)=%d errFlags=%d queueLen=%d", battmV, uptimeS,
					errflags, queueLen);
		case RADIO:
			return String.format("RESP_STATS RADIO noiseFloor=%d lastRssi=%d lastSnr=%.2f txAir(s)=%d rxAir(s)=%d",
					noiseFloor, lastRssi, lastSnr4 / 4.0, txAirSec, rxAirSec);
		case PACKETS:
			return String.format(
					"RESP_STATS PACKETS recv=%d sent=%d sentFlood=%d sentDirect=%d recvFlood=%d recvDirect=%d recvErrors=%d",
					recv, sent, sentFlood, sentDirect, recvFlood, recvDirect, recvErrors);
		}
		return "RESP_STATS unknown subtype";
	}
}
