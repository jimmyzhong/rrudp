
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

import com.hd123.auction.seg.ACKSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.SequenceNumber;
import com.hd123.auction.util.UDPThreadFactory;


public class UDPSender {

	private static final Logger logger=Logger.getLogger(UDPSender.class.getName());

	private final ClientSession session;

	private final Map<Integer,Segment> sendBuffer;
	
	//收到的要发送的数据包
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

	private volatile boolean stopped=false;

	public UDPSender(ClientSession session){
		this.session=session;
		sendBuffer=new ConcurrentHashMap<Integer, Segment>(); 
		sendQueue = new ArrayBlockingQueue<Segment>(1000);  
		lastAckSequenceNumber=-1;
		currentSequenceNumber=-1;
		doStart();
	}

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
		senderThread=UDPThreadFactory.get().newThread(run);
		senderThread.start();
	}

	public void senderAction() throws InterruptedException, IOException{
				//没有收到确认的数据包数量
				int unAcknowledged=unacknowledged.get();
				if(unAcknowledged<10){
					Segment dp=sendQueue.poll();
					if(dp!=null){
						send(dp);
						largestSentSequenceNumber=dp.seq();
					}
				}
	}

	private void send(Segment p)throws IOException{
		synchronized(sendLock){
			session.doSend(p);
			sendBuffer.put(p.seq(), p);
			unacknowledged.incrementAndGet();
		}
	}

	//外部调用
	protected boolean sendUDPPacket(Segment p)throws IOException,InterruptedException{
		return sendQueue.offer(p);
	}

	//用于调节发送速度
	protected void received(Segment p) throws IOException{
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

	//超过一定时间即发送空包用于保持连接
	protected void sendKeepAlive() throws Exception{
	}

	//不加入序列号
	protected void sendAck(int ackSequenceNumber)throws IOException{
		ACKSegment seg = new ACKSegment(0,ackSequenceNumber);
		seg.setSession(session);
		session.doSend(seg);
	}

	private void handleResubmit(){
		
	}
	

	protected void handleResubmit(Integer seqNumber){
		try {
			Segment pack = sendBuffer.get(seqNumber);
			if(pack!=null){
				session.doSend(pack);
			}
		}catch (Exception e) {
			logger.log(Level.WARNING,"",e);
		}
	}


	public long getLargestSentSequenceNumber(){
		return largestSentSequenceNumber;
	}
	
	public long getLastAckSequenceNumber(){
		return lastAckSequenceNumber;
	}

	public void stop(){
		stopped=true;
	}
}
