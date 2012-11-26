package com.hd123.auction;

import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.hd123.auction.seg.Segment;

public abstract class UDPSession {

	private static final Logger logger = Logger.getLogger(UDPSession.class
			.getName());

	protected int mode;
	protected volatile boolean active;
	private volatile int state = CLOSED;
	protected volatile Segment lastPacket;

	public static final int SYN_SENT = 1;
	public static final int SYN_RECEIVE = 2;
	public static final int ESTABLISHED = 3;

	public static final int INVALID = 99;
	public static final int CLOSED = 0;

	protected UDPSocket socket;

	protected int receiveBufferSize = 64 * 32768;

	// 序列号 最大值
	protected int sequenceSize = 0xff;

	public int getSequenceSize() {
		return sequenceSize;
	}

	public void setSequenceSize(int sequenceSize) {
		this.sequenceSize = sequenceSize;
	}

	// 序列号
	protected AtomicInteger sequence = new AtomicInteger(0);

	public int getSequence() {
		if (sequence == null)
			sequence = new AtomicInteger(initialSequenceNumber);
		return sequence.get();
	}

	public void setSequence(int sequence) {
		this.sequence.set(sequence);
	}

	public int incrementSequenceAndGet() {
		for (;;) {
			int current = getSequence();
			int next = current == sequenceSize ? 0 : current + 1;
			if (sequence.compareAndSet(current, next))
				return current;
		}
	}

	private DatagramPacket dgPacket;

	protected final Destination dest;

	public static final int DEFAULT_DATAGRAM_SIZE = 1024 * 4;

	protected int datagramSize = DEFAULT_DATAGRAM_SIZE;

	private int initialSequenceNumber;

	public UDPSession(Destination dest) {
		this.dest = dest;
		this.dgPacket = new DatagramPacket(new byte[0], 0, dest.getAddress(),
				dest.getPort());
	}

	// 获得接受缓存大小，这个值以后可以根据网络状态自动调整
	private int receiverBufferSize = 10;

	public int getReceiverBufferSize() {
		return receiverBufferSize;
	}

	public UDPSocket getSocket() {
		return socket;
	}

	public int getState() {
		return state;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setSocket(UDPSocket socket) {
		this.socket = socket;
	}

	public void setState(int state) {
		logger.info(toString() + " connection state CHANGED to <" + state + ">");
		this.state = state;
	}

	public boolean isReady() {
		return state == ESTABLISHED;
	}

	public boolean isActive() {
		return active == true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isShutdown() {
		return state == CLOSED || state == INVALID;
	}

	public int getDatagramSize() {
		return datagramSize;
	}

	public void setDatagramSize(int datagramSize) {
		this.datagramSize = datagramSize;
	}

	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	public void setReceiveBufferSize(int bufferSize) {
		this.receiveBufferSize = bufferSize;
	}

	public synchronized int getInitialSequenceNumber() {
		return initialSequenceNumber;
	}

	public synchronized void setInitialSequenceNumber(int initialSequenceNumber) {
		this.initialSequenceNumber = initialSequenceNumber;
	}

	public DatagramPacket getDatagram() {
		return dgPacket;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append(" [");
		sb.append(" ]");
		return sb.toString();
	}

}
