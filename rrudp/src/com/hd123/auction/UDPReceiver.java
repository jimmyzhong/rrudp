package com.hd123.auction;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.UDPInputStream.AppData;
import com.hd123.auction.seg.ACKSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.ReceiveBuffer;
import com.hd123.auction.util.UDPThreadFactory;

public class UDPReceiver {

	private static final Logger logger = Logger.getLogger(UDPReceiver.class.getName());

	private final ClientSession session;

	private final List lostList = new LinkedList();
	private ReceiveBuffer receiverBuffer;
	private final BlockingQueue<Segment> handoffQueue;

	private Thread receiverThread;

	private volatile boolean stopped = false;

	public UDPReceiver(ClientSession session) {
		this.session = session;
		receiverBuffer = new ReceiveBuffer(8, session.getSequenceSize(), session.getInitialSequenceNumber());
		handoffQueue = new ArrayBlockingQueue<Segment>(18);
		start();
	}

	private void start() {
		Runnable r = new Runnable() {
			public void run() {
				try {
					while (!stopped) {
						receiverAlgorithm();
					}
				} catch (Exception ex) {
					logger.log(Level.SEVERE, "接受线程错误", ex);
				}
				logger.info("STOPPING RECEIVER for " + session);
			}
		};
		receiverThread = UDPThreadFactory.get().newThread(r);
		receiverThread.start();
	}

	protected void received(Segment p) throws IOException {
		handoffQueue.offer(p);
	}

	public void receiverAlgorithm() throws InterruptedException, IOException {
		Segment packet = handoffQueue.poll();
		if (packet != null) {
			processUDTPacket(packet);
		}
		Thread.yield();
	}

	protected void processUDTPacket(Segment p) throws IOException {
		AppData data = new AppData(p.seq(), p.getBytes());
		receiverBuffer.offer(data);
		sendAcknowledgment(p.seq());
	}

	protected void sendAcknowledgment(int currentSequenceNumber) throws IOException {
		ACKSegment ack = new ACKSegment(0, currentSequenceNumber);
		ack.setSession(session);
		session.doSend(ack);
	}

	protected void sendShutdown() throws IOException {
	}

	public void stop() {
		stopped = true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("UDPReceiver ").append(session).append("\n");
		sb.append("LossList: " + lostList);
		return sb.toString();
	}

}
