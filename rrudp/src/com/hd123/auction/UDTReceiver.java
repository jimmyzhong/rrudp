
package com.hd123.auction;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.receiver.ReceiverLossList;
import com.hd123.auction.util.UDTThreadFactory;

public class UDTReceiver {

	private static final Logger logger=Logger.getLogger(UDTReceiver.class.getName());

	private final UDPEndPoint endpoint;

	private final UDTSession session;

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

	//EXP event related

	//a variable to record number of continuous EXP time-out events 
	private volatile long expCount=0;

	/*records the time interval between each probing pair
    compute the median packet pair interval of the last
	16 packet pair intervals (PI) and the estimate link capacity.(packet/s)*/
//	private final PacketPairWindow packetPairWindow;

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
	private long ackTimerInterval=Util.getSYNTime();

	private long nextNAK;
	//microseconds to next NAK event
	private long nakTimerInterval=Util.getSYNTime();

	private long nextEXP;
	//microseconds to next EXP event
	private long expTimerInterval=100*Util.getSYNTime();

	//instant when the session was created (for expiry checking)
	private final long sessionUpSince;
	//milliseconds to timeout a new session that stays idle
	private final long IDLE_TIMEOUT = 3*60*1000;

	//buffer size for storing data
	private final long bufferSize;

	//stores received packets to be sent
	private final BlockingQueue<UDTPacket> handoffQueue;

	private Thread receiverThread;

	private volatile boolean stopped=false;

	//(optional) ack interval (see CongestionControl interface)
	private volatile long ackInterval=-1;

	/**
	 * if set to true connections will not expire, but will only be
	 * closed by a Shutdown message
	 */
	public static boolean connectionExpiryDisabled=false;

	private final boolean storeStatistics;
	
	public UDTReceiver(UDTSession session,UDPEndPoint endpoint){
		this.endpoint = endpoint;
		this.session=session;
		this.sessionUpSince=System.currentTimeMillis();
		if(!session.isReady())
			throw new IllegalStateException("UDPSession is not ready.");
		ackHistoryWindow = new AckHistoryWindow(16);
		packetHistoryWindow = new PacketHistoryWindow(16);
		receiverLossList = new ReceiverLossList();
		packetPairWindow = new PacketPairWindow(16);
		largestReceivedSeqNumber=session.getInitialSequenceNumber()-1;
		bufferSize=session.getReceiveBufferSize();
		handoffQueue=new ArrayBlockingQueue<UDTPacket>(4*session.getFlowWindowSize());
		storeStatistics=Boolean.getBoolean("udt.receiver.storeStatistics");
		initMetrics();
		start();
	}
	


	//starts the sender algorithm
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

	/*
	 * packets are written by the endpoint
	 */
	protected void receive(UDTPacket p)throws IOException{
		if(storeStatistics)dgReceiveInterval.end();
		handoffQueue.offer(p);
		if(storeStatistics)dgReceiveInterval.begin();
	}

	/**
	 * receiver algorithm 
	 * see specification P11.
	 */
	public void receiverAlgorithm()throws InterruptedException,IOException{
		//check ACK timer
		long currentTime=Util.getCurrentTime();
		//perform time-bounded UDP receive
		UDTPacket packet=handoffQueue.poll(Util.getSYNTime(), TimeUnit.MICROSECONDS);
		if(packet!=null){
			//reset exp count to 1
			expCount=1;
			//If there is no unacknowledged data packet, or if this is an 
			//ACK or NAK control packet, reset the EXP timer.
			boolean needEXPReset=false;
			if(packet.isControlPacket()){
				ControlPacket cp=(ControlPacket)packet;
				int cpType=cp.getControlPacketType();
				if(cpType==ControlPacketType.ACK.ordinal() || cpType==ControlPacketType.NAK.ordinal()){
					needEXPReset=true;
				}
			}
			if(needEXPReset){
				nextEXP=Util.getCurrentTime()+expTimerInterval;
			}
			if(storeStatistics)processTime.begin();
			
			processUDTPacket(packet);
			
			if(storeStatistics)processTime.end();
		}
		
		Thread.yield();
	}

	protected void processUDTPacket(UDTPacket p)throws IOException{
		//(3).Check the packet type and process it according to this.
		
		if(!p.isControlPacket()){
			DataPacket dp=(DataPacket)p;
			if(storeStatistics){
				dataPacketInterval.end();
				dataProcessTime.begin();
			}
			onDataPacketReceived(dp);
			if(storeStatistics){
				dataProcessTime.end();
				dataPacketInterval.begin();
			}
		}

		else if (p.getControlPacketType()==ControlPacketType.ACK2.ordinal()){
			Acknowledgment2 ack2=(Acknowledgment2)p;
			onAck2PacketReceived(ack2);
		}

		else if (p instanceof Shutdown){
			onShutdown();
		}

	}

	//every nth packet will be discarded... for testing only of course
	public static int dropRate=0;
	
	//number of received data packets
	private int n=0;
	
	protected void onDataPacketReceived(DataPacket dp)throws IOException{
		long currentSequenceNumber = dp.getPacketSequenceNumber();
		
		//for TESTING : check whether to drop this packet
//		n++;
//		//if(dropRate>0 && n % dropRate == 0){
//			if(n % 1111 == 0){	
//				logger.info("**** TESTING:::: DROPPING PACKET "+currentSequenceNumber+" FOR TESTING");
//				return;
//			}
//		//}
		boolean OK=session.getSocket().getInputStream().haveNewData(currentSequenceNumber,dp.getData());
		if(!OK){
			//need to drop packet...
			return;
		}
		
		long currentDataPacketArrivalTime = Util.getCurrentTime();

		/*(4).if the seqNo of the current data packet is 16n+1,record the
		time interval between this packet and the last data packet
		in the packet pair window*/
		if((currentSequenceNumber%16)==1 && lastDataPacketArrivalTime>0){
			long interval=currentDataPacketArrivalTime -lastDataPacketArrivalTime;
			packetPairWindow.add(interval);
		}
		
		//(5).record the packet arrival time in the PKT History Window.
		packetHistoryWindow.add(currentDataPacketArrivalTime);

		
		//store current time
		lastDataPacketArrivalTime=currentDataPacketArrivalTime;

		
		//(6).number of detected lossed packet
		/*(6.a).if the number of the current data packet is greater than LSRN+1,
			put all the sequence numbers between (but excluding) these two values
			into the receiver's loss list and send them to the sender in an NAK packet*/
		if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber+1)>0){
			sendNAK(currentSequenceNumber);
		}
		else if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber)<0){
				/*(6.b).if the sequence number is less than LRSN,remove it from
				 * the receiver's loss list
				 */
				receiverLossList.remove(currentSequenceNumber);
		}

		statistics.incNumberOfReceivedDataPackets();

		//(7).Update the LRSN
		if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber)>0){
			largestReceivedSeqNumber=currentSequenceNumber;
		}

		//(8) need to send an ACK? Some cc algorithms use this
		if(ackInterval>0){
			if(n % ackInterval == 0)processACKEvent(false);
		}
	}

	/**
	 * write a NAK triggered by a received sequence number that is larger than
	 * the largestReceivedSeqNumber + 1
	 * @param currentSequenceNumber - the currently received sequence number
	 * @throws IOException
	 */
	protected void sendNAK(long currentSequenceNumber)throws IOException{
		NegativeAcknowledgement nAckPacket= new NegativeAcknowledgement();
		nAckPacket.addLossInfo(largestReceivedSeqNumber+1, currentSequenceNumber);
		nAckPacket.setSession(session);
		nAckPacket.setDestinationID(session.getDestination().getSocketID());
		//put all the sequence numbers between (but excluding) these two values into the
		//receiver loss list
		for(long i=largestReceivedSeqNumber+1;i<currentSequenceNumber;i++){
			ReceiverLossListEntry detectedLossSeqNumber= new ReceiverLossListEntry(i);
			receiverLossList.insert(detectedLossSeqNumber);
		}
		endpoint.doSend(nAckPacket);
		//logger.info("NAK for "+currentSequenceNumber);
		statistics.incNumberOfNAKSent();
	}

	protected void sendNAK(List<Long>sequenceNumbers)throws IOException{
		if(sequenceNumbers.size()==0)return;
		NegativeAcknowledgement nAckPacket= new NegativeAcknowledgement();
		nAckPacket.addLossInfo(sequenceNumbers);
		nAckPacket.setSession(session);
		nAckPacket.setDestinationID(session.getDestination().getSocketID());
		endpoint.doSend(nAckPacket);
		statistics.incNumberOfNAKSent();
	}

	protected long sendLightAcknowledgment(long ackNumber)throws IOException{
		Acknowledgement acknowledgmentPkt=buildLightAcknowledgement(ackNumber);
		endpoint.doSend(acknowledgmentPkt);
		statistics.incNumberOfACKSent();
		return acknowledgmentPkt.getAckSequenceNumber();
	}

	protected long sendAcknowledgment(long ackNumber)throws IOException{
		Acknowledgement acknowledgmentPkt = buildLightAcknowledgement(ackNumber);
		//set the estimate link capacity
		estimateLinkCapacity=packetPairWindow.getEstimatedLinkCapacity();
		acknowledgmentPkt.setEstimatedLinkCapacity(estimateLinkCapacity);
		//set the packet arrival rate
		packetArrivalSpeed=packetHistoryWindow.getPacketArrivalSpeed();
		acknowledgmentPkt.setPacketReceiveRate(packetArrivalSpeed);

		endpoint.doSend(acknowledgmentPkt);

		statistics.incNumberOfACKSent();
		statistics.setPacketArrivalRate(packetArrivalSpeed, estimateLinkCapacity);
		return acknowledgmentPkt.getAckSequenceNumber();
	}

	//builds a "light" Acknowledgement
	private Acknowledgement buildLightAcknowledgement(long ackNumber){
		Acknowledgement acknowledgmentPkt = new Acknowledgement();
		//the packet sequence number to which all the packets have been received
		acknowledgmentPkt.setAckNumber(ackNumber);
		//assign this ack a unique increasing ACK sequence number
		acknowledgmentPkt.setAckSequenceNumber(++ackSequenceNumber);
		acknowledgmentPkt.setRoundTripTime(roundTripTime);
		acknowledgmentPkt.setRoundTripTimeVar(roundTripTimeVar);
		//set the buffer size
		acknowledgmentPkt.setBufferSize(bufferSize);

		acknowledgmentPkt.setDestinationID(session.getDestination().getSocketID());
		acknowledgmentPkt.setSession(session);

		return acknowledgmentPkt;
	}

	/**
	 * spec p. 13: <br/>
	  1) Locate the related ACK in the ACK History Window according to the 
         ACK sequence number in this ACK2.  <br/>
      2) Update the largest ACK number ever been acknowledged. <br/>
      3) Calculate new rtt according to the ACK2 arrival time and the ACK 
         departure time, and update the RTT value as: RTT = (RTT * 7 + 
         rtt) / 8.  <br/>
      4) Update RTTVar by: RTTVar = (RTTVar * 3 + abs(RTT - rtt)) / 4.  <br/>
      5) Update both ACK and NAK period to 4 * RTT + RTTVar + SYN.  <br/>
	 */
	protected void onAck2PacketReceived(Acknowledgment2 ack2){
		AckHistoryEntry entry=ackHistoryWindow.getEntry(ack2.getAckSequenceNumber());
		if(entry!=null){
			long ackNumber=entry.getAckNumber();
			largestAcknowledgedAckNumber=Math.max(ackNumber, largestAcknowledgedAckNumber);
			
			long rtt=entry.getAge();
			if(roundTripTime>0)roundTripTime = (roundTripTime*7 + rtt)/8;
			else roundTripTime = rtt;
			roundTripTimeVar = (roundTripTimeVar* 3 + Math.abs(roundTripTimeVar- rtt)) / 4;
			ackTimerInterval=4*roundTripTime+roundTripTimeVar+Util.getSYNTime();
			nakTimerInterval=ackTimerInterval;
			statistics.setRTT(roundTripTime, roundTripTimeVar);
		}
	}

	protected void sendKeepAlive()throws IOException{
		KeepAlive ka=new KeepAlive();
		ka.setDestinationID(session.getDestination().getSocketID());
		ka.setSession(session);
		endpoint.doSend(ka);
	}

	protected void sendShutdown()throws IOException{
		Shutdown s=new Shutdown();
		s.setDestinationID(session.getDestination().getSocketID());
		s.setSession(session);
		endpoint.doSend(s);
	}

	private volatile long ackSequenceNumber=0;

	protected void resetEXPTimer(){
		nextEXP=Util.getCurrentTime()+expTimerInterval;
		expCount=0;
	}

	protected void resetEXPCount(){
		expCount=0;
	}
	
	public void setAckInterval(long ackInterval){
		this.ackInterval=ackInterval;
	}
	
	protected void onShutdown()throws IOException{
		stop();
	}

	public void stop()throws IOException{
		stopped=true;
		session.getSocket().close();
		//stop our sender as well
		session.getSocket().getSender().stop();
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("UDTReceiver ").append(session).append("\n");
		sb.append("LossList: "+receiverLossList);
		return sb.toString();
	}

}
