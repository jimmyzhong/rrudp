package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.hd123.auction.seg.Segment;


public abstract class UDPSession {

	private static final Logger logger=Logger.getLogger(UDPSession.class.getName());

	protected int mode;
	protected volatile boolean active;
	private volatile int state=START;
	protected volatile Segment lastPacket;
	
	public static final int START=0;
	public static final int HANDSHARKING=1;
	public static final int READY=2;
	public static final int KEEPLIVE=3;
	public static final int SHUTDOWN=4;
	
	public static final int INVALID=99;

	protected volatile UDPSocket socket;
	
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

	protected final Destination dest;
	
	protected int localPort;
	
	
	public static final int DEFAULT_DATAGRAM_SIZE=1024*4;
	
	protected int datagramSize=DEFAULT_DATAGRAM_SIZE;
	
	protected int initialSequenceNumber;
	
	
	public UDPSession(Destination peer){
		this.dest = peer;
		this.dgPacket=new DatagramPacket(new byte[0],0,peer.getAddress(),peer.getPort());
	}
	
	public abstract void received(Segment packet);
	
	//获得接受缓存大小，这个值以后可以根据网络状态自动调整
	private int receiverBufferSize = 10;
	public int getReceiverBufferSize(){
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
		return state==SHUTDOWN || state==INVALID;
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

	public synchronized int getInitialSequenceNumber(){
		return initialSequenceNumber;
	}
	
	public synchronized void setInitialSequenceNumber(int initialSequenceNumber){
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
