package com.hd123.auction;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.ACKSegment;
import com.hd123.auction.seg.DATSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.UDPThreadFactory;

public class UDPSender {

	private static final Logger logger = Logger.getLogger(UDPSender.class.getName());

	private final ClientSession session;

	private final Map<Integer, Segment> sendBuffer;

	// 收到的要发送的数据包
	private final BlockingQueue<Segment> sendQueue;

	private Thread senderThread;

	private final Object sendLock = new Object();

	private final AtomicInteger unacknowledged = new AtomicInteger(0);

	private volatile boolean stopped = false;

	public UDPSender(ClientSession session) {
		this.session = session;
		sendBuffer = new ConcurrentHashMap<Integer, Segment>();
		sendQueue = new ArrayBlockingQueue<Segment>(1000);
	}

	public void start() {
		Runnable run = new Runnable() {
			public void run() {
				try {
					while (!stopped) {
						senderAction();
					}
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
					logger.log(Level.SEVERE, "数据发送错误", ex);
				}
			}
		};
		senderThread = UDPThreadFactory.get().newThread(run);
		senderThread.start();
	}

	public void senderAction() throws InterruptedException, IOException {
		// 没有收到确认的数据包数量
		// int unAcknowledged=unacknowledged.get();
		// if(unAcknowledged<10){
		//等待建立连接
		while (session.getState() != UDPSession.ESTABLISHED )
			synchronized (session.connectedSyn) {
				session.connectedSyn.wait();
			}
		Segment dp = sendQueue.take();
		if (dp != null) {
			send(dp);
		}
		// }
	}

	private void send(Segment p) throws IOException {
		synchronized (sendLock) {
			session.doSend(p);
			sendBuffer.put(p.seq(), p);
			unacknowledged.incrementAndGet();
		}
	}

	// 外部调用
	protected void sendUDPPacket(Segment p) throws IOException, InterruptedException {
		sendQueue.put(p);
	}

	// 用于调节发送速度 清楚发送缓存中的数据
	protected void received(Segment p) throws IOException {
		onAcknowledge(p);
	}

	protected void onAcknowledge(Segment pack) throws IOException {
		int id = pack.getAck();
		if (!(pack instanceof DATSegment))
			return;
		boolean removed = false;
		synchronized (sendLock) {
			// 移除发送缓存中的数据
			removed = sendBuffer.remove(id) != null;
		}
		if (removed) {
			unacknowledged.decrementAndGet();
		}
	}

	// 超过一定时间即发送空包用于保持连接
	protected void sendKeepAlive() throws Exception {
	}

	// 不加入序列号
	protected void sendAck(int ackSequenceNumber) throws IOException {
		ACKSegment seg = new ACKSegment(0, ackSequenceNumber);
		seg.setSession(session);
		session.doSend(seg);
	}

	private void handleResubmit() {

	}

	protected void handleResubmit(Integer seqNumber) {
		try {
			Segment pack = sendBuffer.get(seqNumber);
			if (pack != null) {
				session.doSend(pack);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
		}
	}

	public void stop() {
		stopped = true;
	}
}
