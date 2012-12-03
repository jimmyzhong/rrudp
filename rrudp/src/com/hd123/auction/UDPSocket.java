package com.hd123.auction;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.hd123.auction.seg.DATSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.ReceiveBuffer;

public class UDPSocket {

	private volatile boolean active;

	private UDPReceiver receiver;
	private UDPSender sender;

	private final ClientSession session;

	private UDPInputStream inputStream;
	private UDPOutputStream outputStream;
	
	private volatile ReceiveBuffer receiverBuffer;

	public UDPSocket(ClientSession session) throws SocketException,
			UnknownHostException {
		this.session = session;
		this.receiverBuffer = new ReceiveBuffer(session.getReceiverBufferSize(), session.getSequenceSize());
		this.receiver = new UDPReceiver(session,receiverBuffer);
		this.sender = new UDPSender(session);
	}

	// 用户发送数据 通过InputStream入口
	public void doWrite(byte[] data) throws IOException {
		int next = receiver.getLastSegmentSeq();
		DATSegment packet = new DATSegment(session.incrementSequenceAndGet(),next,data);
		packet.setSession(session);
		try {
			sender.sendUDPPacket(packet);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void close() throws IOException {
		active = false;
	}

	public synchronized UDPInputStream getInputStream() throws IOException {
		if (inputStream == null) {
			inputStream = new UDPInputStream(this);
		}
		return inputStream;
	}

	public synchronized UDPOutputStream getOutputStream() {
		if (outputStream == null) {
			outputStream = new UDPOutputStream(this);
		}
		return outputStream;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public final ClientSession getSession() {
		return session;
	}

	public void start() {
		receiver.start();
		sender.start();
	}
	
	public void stop() {
		receiver.stop();
		sender.stop();
	}

	public void received(Segment packet) {
		try {
			// 可用于调节发送速度
			receiver.received(packet);
			// 可用于发送没有收到的数据包的ACK
			sender.received(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ReceiveBuffer getReceiverBuffer() {
		return receiverBuffer;
	}

	public void setInitialDataSequenceNumber(int i) {
		receiverBuffer.setInitialDataSequenceNumber(i);
	}

}
