package com.hd123.auction;

import java.net.DatagramPacket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.Segment;


public abstract class UDTSession {

	private static final Logger logger=Logger.getLogger(UDTSession.class.getName());

	protected int mode;
	protected volatile boolean active;
	private volatile int state=start;
	protected volatile Segment lastPacket;
	
	//state constants	
	public static final int start=0;
	public static final int handshaking=1;
	public static final int ready=2;
	public static final int keepalive=3;
	public static final int shutdown=4;
	
	public static final int invalid=99;

	protected volatile UDTSocket socket;
	
//	protected final UDTStatistics statistics;
	
	protected int receiveBufferSize=64*32768;
	
	protected int sequenceSize = 0xff;
	
//	protected final CongestionControl cc;
	
	public int getSequenceSize() {
		return sequenceSize;
	}


	public void setSequenceSize(int sequenceSize) {
		this.sequenceSize = sequenceSize;
	}

	//cache dgPacket (peer stays the same always)
	private DatagramPacket dgPacket;

	/**
	 * flow window size, i.e. how many data packets are
	 * in-flight at a single time
	 */
	protected int flowWindowSize=1024;

	/**
	 * remote UDT entity (address and socket ID)
	 */
	protected final Destination destination;
	
	/**
	 * local port
	 */
	protected int localPort;
	
	
	public static final int DEFAULT_DATAGRAM_SIZE=1024*4;
	
	/**
	 * key for a system property defining the CC class to be used
	 * @see CongestionControl
	 */
//	public static final String CC_CLASS="udt.congestioncontrol.class";
	
	/**
	 * Buffer size (i.e. datagram size)
	 * This is negotiated during connection setup
	 */
	protected int datagramSize=DEFAULT_DATAGRAM_SIZE;
	
	protected Long initialSequenceNumber=null;
	
	
	private final static AtomicLong nextSocketID=new AtomicLong(20+new Random().nextInt(5000));
	
	private final AtomicInteger seqID = new AtomicInteger();
	
	public UDTSession(String description, Destination destination){
		this.destination=destination;
		this.dgPacket=new DatagramPacket(new byte[0],0,destination.getAddress(),destination.getPort());
	}
	
	
	public abstract void received(Segment packet, Destination peer);
	
	
	public UDTSocket getSocket() {
		return socket;
	}

	public int getState() {
		return state;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setSocket(UDTSocket socket) {
		this.socket = socket;
	}

	public void setState(int state) {
		logger.info(toString()+" connection state CHANGED to <"+state+">");
		this.state = state;
	}
	
	public boolean isReady(){
		return state==ready;
	}

	public boolean isActive() {
		return active == true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public boolean isShutdown(){
		return state==shutdown || state==invalid;
	}
	
	public Destination getDestination() {
		return destination;
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

	public int getFlowWindowSize() {
		return flowWindowSize;
	}

	public void setFlowWindowSize(int flowWindowSize) {
		this.flowWindowSize = flowWindowSize;
	}

	public synchronized long getInitialSequenceNumber(){
		if(initialSequenceNumber==null){
			initialSequenceNumber=1l; //TODO must be random?
		}
		return initialSequenceNumber;
	}
	
	public synchronized void setInitialSequenceNumber(long initialSequenceNumber){
		this.initialSequenceNumber=initialSequenceNumber;
	}

	public DatagramPacket getDatagram(){
		return dgPacket;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(super.toString());
		sb.append(" [");
		sb.append(" ]");
		return sb.toString();
	}
	
}
