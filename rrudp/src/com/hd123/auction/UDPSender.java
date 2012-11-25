
package com.hd123.auction;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.SequenceNumber;
import com.hd123.auction.util.UDTThreadFactory;


public class UDPSender {

	private static final Logger logger=Logger.getLogger(UDPSender.class.getName());

	private final ServerSocketImpl endpoint;

	private final UDPSession session;

	private final Map<Integer,Segment> sendBuffer;
	
	//sendQueue contains the packets to send
	private final BlockingQueue<Segment> sendQueue;
	
	//thread reading packets from send queue and sending them
	private Thread senderThread;

	//protects against races when reading/writing to the sendBuffer
	private final Object sendLock=new Object();

	//number of unacknowledged data packets
	private final AtomicInteger unacknowledged=new AtomicInteger(0);

	//for generating data packet sequence numbers
	private volatile int currentSequenceNumber=0;

	//the largest data packet sequence number that has actually been sent out
	private volatile int largestSentSequenceNumber=-1;

	//last acknowledge number, initialised to the initial sequence number
	private volatile int lastAckSequenceNumber;

	private volatile boolean started=false;

	private volatile boolean stopped=false;

	private volatile boolean paused=false;

	//used to signal that the sender should start to send
	private volatile CountDownLatch startLatch=new CountDownLatch(1);

	//used by the sender to wait for an ACK
	private final AtomicReference<CountDownLatch> waitForAckLatch=new AtomicReference<CountDownLatch>();

	//used by the sender to wait for an ACK of a certain sequence number
	private final AtomicReference<CountDownLatch> waitForSeqAckLatch=new AtomicReference<CountDownLatch>();

	public UDPSender(UDPSession session,ServerSocketImpl endpoint){
		if(!session.isReady())
			throw new IllegalStateException("UDPSession is not ready.");
		this.endpoint= endpoint;
		this.session=session;
		sendBuffer=new ConcurrentHashMap<Integer, Segment>(); 
		sendQueue = new ArrayBlockingQueue<Segment>(1000);  
		lastAckSequenceNumber=-1;
		currentSequenceNumber=-1;
		waitForAckLatch.set(new CountDownLatch(1));
		waitForSeqAckLatch.set(new CountDownLatch(1));
		doStart();
	}

	/**
	 * start the sender thread
	 */
	public void start(){
		logger.info("Starting sender for "+session);
		startLatch.countDown();
		started=true;
	}

	//starts the sender algorithm
	private void doStart(){
		Runnable run=new Runnable(){
			public void run(){
				try{
					while(!stopped){
						senderAction();
					}
				}catch(InterruptedException ie){
					ie.printStackTrace();
				}
				catch(IOException ex){
					ex.printStackTrace();
					logger.log(Level.SEVERE,"数据发送错误",ex);
				}
			}
		};
		senderThread=UDTThreadFactory.get().newThread(run);
		senderThread.start();
	}


	private void send(Segment p)throws IOException{
		synchronized(sendLock){
			endpoint.doSend(p);
			sendBuffer.put(p.seq(), p);
			unacknowledged.incrementAndGet();
		}
	}

	protected boolean sendUDPPacket(Segment p)throws IOException,InterruptedException{
		if(!started)
			start();
		return sendQueue.offer(p);
	}

	//用于调节发送速度
	protected void receive(Segment p)throws IOException{
//		if (p instanceof Acknowledgement) {
//			Acknowledgement acknowledgement=(Acknowledgement)p;
//			onAcknowledge(acknowledgement);
//		}
//		else if (p instanceof NegativeAcknowledgement) {
//			NegativeAcknowledgement nak=(NegativeAcknowledgement)p;
//			onNAKPacketReceived(nak);
//		}
//		else if (p instanceof KeepAlive) {
//			session.getSocket().getReceiver().resetEXPCount();
//		}
	}

	protected void onAcknowledge(Segment pack)throws IOException{
		int  id = pack.getAck();
		if(id < 0)
			return;
		boolean removed = false;
		synchronized (sendLock) {
			//移除发送缓存中的数据
				removed=sendBuffer.remove(id)!=null;
		}
		if(removed){
				unacknowledged.decrementAndGet();
		}
	}

	protected void sendKeepAlive() throws Exception{
//		KeepAlive keepAlive = new KeepAlive();
//		keepAlive.setSession(session);
//		endpoint.doSend(keepAlive);
	}

	protected void sendAck2(long ackSequenceNumber)throws IOException{
//		Acknowledgment2 ackOfAckPkt = new Acknowledgment2();
//		ackOfAckPkt.setAckSequenceNumber(ackSequenceNumber);
//		ackOfAckPkt.setSession(session);
//		ackOfAckPkt.setDestinationID(session.getDestination().getSocketID());
//		endpoint.doSend(ackOfAckPkt);
	}

	public void senderAction() throws InterruptedException, IOException{
		while(!paused){
				//没有收到确认的数据包数量
				int unAcknowledged=unacknowledged.get();
				if(unAcknowledged<10){
					Segment dp=sendQueue.poll();
					if(dp!=null){
						send(dp);
						unacknowledged.decrementAndGet();
						largestSentSequenceNumber=dp.seq();
					}
				}else{
					waitForAck();
				}
			}
	}

	protected void handleResubmit(Integer seqNumber){
		try {
			//retransmit the packet and remove it from  the list
			Segment pktToRetransmit = sendBuffer.get(seqNumber);
			if(pktToRetransmit!=null){
				endpoint.doSend(pktToRetransmit);
			}
		}catch (Exception e) {
			logger.log(Level.WARNING,"",e);
		}
	}

	public int getNextSequenceNumber(){
		currentSequenceNumber=SequenceNumber.increment(currentSequenceNumber);
		return currentSequenceNumber;
	}

	public int getCurrentSequenceNumber(){
		return currentSequenceNumber;
	}

	/**
	 * returns the largest sequence number sent so far
	 */
	public long getLargestSentSequenceNumber(){
		return largestSentSequenceNumber;
	}
	/**
	 * returns the last Ack. sequence number 
	 */
	public long getLastAckSequenceNumber(){
		return lastAckSequenceNumber;
	}

	boolean haveAcknowledgementFor(long sequenceNumber){
		return SequenceNumber.compare(sequenceNumber,lastAckSequenceNumber)<=0;
	}

	boolean isSentOut(long sequenceNumber){
		return SequenceNumber.compare(largestSentSequenceNumber,sequenceNumber)>=0;
	}

	public void waitForAck()throws InterruptedException{
		waitForAckLatch.set(new CountDownLatch(1));
		waitForAckLatch.get().await(2, TimeUnit.MILLISECONDS);
	}


	public void stop(){
		stopped=true;
	}
	
	public void pause(){
		startLatch=new CountDownLatch(1);
		paused=true;
	}
}
