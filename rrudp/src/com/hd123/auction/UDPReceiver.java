
package com.hd123.auction;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.receiver.ReceiverLossList;
import com.hd123.auction.seg.ACKSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.UDTThreadFactory;

public class UDPReceiver {

	private static final Logger logger=Logger.getLogger(UDPReceiver.class.getName());

	private final UDPEndPoint endpoint;

	private final UDPSession session;

	private final ReceiverLossList receiverLossList;

	//record each sent ACK and the sent time 
//	private final AckHistoryWindow ackHistoryWindow;

	//Packet history window that stores the time interval between the current and the last seq.
//	private final PacketHistoryWindow packetHistoryWindow;

	//for storing the arrival time of the last received data packet
	private volatile long lastDataPacketArrivalTime=0;

	//largest received data packet sequence number(LRSN)
	private volatile long largestReceivedSeqNumber=0;

	//ACK event related

	//last Ack number
	private long lastAckNumber=0;

	//largest Ack number ever acknowledged by ACK2
	private volatile long largestAcknowledgedAckNumber=-1;


	//a variable to record number of continuous EXP time-out events 
	private volatile long expCount=0;
//	private final PacketPairWindow    packetPairWindow;

	//estimated link capacity
	long estimateLinkCapacity;
	// the packet arrival rate
	long packetArrivalSpeed;

	//round trip time, calculated from ACK/ACK2 pairs
	long roundTripTime=0;
	//round trip time variance
	long roundTripTimeVar=roundTripTime/2;

	//to check the ACK, NAK, or EXP timer
	private long nextACK;
	//microseconds to next ACK event

	//instant when the session was created (for expiry checking)
	private final long sessionUpSince;
	//milliseconds to timeout a new session that stays idle
	private final long IDLE_TIMEOUT = 3*60*1000;

	//buffer size for storing data
	private final long bufferSize;

	//stores received packets to be sent
	private final BlockingQueue<Segment> handoffQueue;

	private Thread receiverThread;

	private volatile boolean stopped=false;

	//(optional) ack interval (see CongestionControl interface)
	private volatile long ackInterval=-1;

	/**
	 * if set to true connections will not expire, but will only be
	 * closed by a Shutdown message
	 */
	public static boolean connectionExpiryDisabled=false;

	public UDPReceiver(UDPSession session,UDPEndPoint endpoint){
		this.endpoint = endpoint;
		this.session=session;
		this.sessionUpSince=System.currentTimeMillis();
		if(!session.isReady())
			throw new IllegalStateException("UDPSession is not ready.");
		receiverLossList = new ReceiverLossList();
		largestReceivedSeqNumber=session.getInitialSequenceNumber()-1;
		bufferSize=session.getReceiveBufferSize();
		handoffQueue=new ArrayBlockingQueue<Segment>(4*session.getFlowWindowSize());
		start();
	}
	
	private void start(){
		Runnable r=new Runnable(){
			public void run(){
				try{
					while(!stopped){
						receiverAlgorithm();
					}
				}
				catch(Exception ex){
					logger.log(Level.SEVERE,"接受线程错误",ex);
				}
				logger.info("STOPPING RECEIVER for "+session);
			}
		};
		receiverThread=UDTThreadFactory.get().newThread(r);
		receiverThread.start();
	}

	protected void receive(Segment p)throws IOException{
		handoffQueue.offer(p);
	}

	public void receiverAlgorithm() throws InterruptedException,IOException{
		Segment packet=handoffQueue.poll();
		if(packet!=null){
			processUDTPacket(packet);
		}
		Thread.yield();
	}

	protected void processUDTPacket(Segment p)throws IOException{


	}

	//every nth packet will be discarded... for testing only of course
	public static int dropRate=0;
	
	//number of received data packets
	private int n=0;
	
	protected void onDataPacketReceived(Segment dp)throws IOException{
		int currentSequenceNumber = dp.seq();
		
		boolean OK=session.getSocket().getInputStream().haveNewData(currentSequenceNumber,dp.getBytes());
		if(!OK){
			return;
		}

	}

	protected void sendAcknowledgment(int currentSequenceNumber) throws IOException{
		ACKSegment nAckPacket= new ACKSegment();
		nAckPacket.setSession(session);
		endpoint.doSend(nAckPacket);
	}

	protected void sendKeepAlive() throws IOException{
	}

	protected void sendShutdown()throws IOException{
	}

	private volatile long ackSequenceNumber=0;

	protected void onShutdown()throws IOException{
		stop();
	}

	public void stop()throws IOException{
		stopped=true;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("UDTReceiver ").append(session).append("\n");
		sb.append("LossList: "+receiverLossList);
		return sb.toString();
	}

}
