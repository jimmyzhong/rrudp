package com.hd123.auction;

import java.net.DatagramPacket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.hd123.auction.seg.Segment;


public abstract class UDTSession {

	private static final Logger logger=Logger.getLogger(UDTSession.class.getName());

	protected int mode;
	protected volatile boolean active;
	private volatile int state=START;
	protected volatile Segment lastPacket;
	
	public static final int START=0;
	public static final int HANDSHARKING=1;
	public static final int READY=2;
	public static final int KEEPLIVE=3;
	public static final int SHUTDOWN=4;
	
	public static final int invalid=99;

	protected volatile UDTSocket socket;
	
	protected int receiveBufferSize=64*32768;
	
	protected int sequenceSize = 0xff;
	
	public int getSequenceSize() {
		return sequenceSize;
	}


	public void setSequenceSize(int sequenceSize) {
		this.sequenceSize = sequenceSize;
	}

	private DatagramPacket dgPacket;

	protected int flowWindowSize=1024;

	protected final Destination destination;
	
	protected int localPort;
	
	
	public static final int DEFAULT_DATAGRAM_SIZE=1024*4;
	
	protected int datagramSize=DEFAULT_DATAGRAM_SIZE;
	
	protected Long initialSequenceNumber=null;
	
	
	public UDTSession(String description, Destination destination){
		this.destination=destination;
		this.dgPacket=new DatagramPacket(new byte[0],0,destination.getAddress(),destination.getPort());
	}
	
	
	public abstract void received(Segment packet, Destination peer);
	
	//获得接受缓存大小，这个值以后可以根据网络状态自动调整
	private int receiverBufferSize = 10;
	public int getReceiverBufferSize(){
		return receiverBufferSize;
	}
	
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
		return state==READY;
	}

	public boolean isActive() {
		return active == true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public boolean isShutdown(){
		return state==SHUTDOWN || state==invalid;
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
