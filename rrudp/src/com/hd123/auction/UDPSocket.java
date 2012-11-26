package com.hd123.auction;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.hd123.auction.seg.DATSegment;
import com.hd123.auction.seg.Segment;

public class UDPSocket {

	private volatile boolean active;

	private UDPReceiver receiver;
	private UDPSender sender;

	private final ClientSession session;

	private UDPInputStream inputStream;
	private UDPOutputStream outputStream;

	public UDPSocket(ClientSession session) throws SocketException,
			UnknownHostException {
		this.session = session;
		this.receiver = new UDPReceiver(session);
		this.sender = new UDPSender(session);
	}

	// 用户发送数据 通过InputStream入口
	public void doWrite(byte[] data) throws IOException {
		DATSegment packet = new DATSegment();
		int seqNo = session.incrementSequenceAndGet();
		packet.seq(seqNo);
		packet.setSession(session);
		packet.setData(data);
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

}
