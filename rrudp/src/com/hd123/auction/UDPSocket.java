
package com.hd123.auction;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.hd123.auction.seg.DATSegment;

public class UDPSocket {
	
	private final UDPEndPoint endpoint;
	
	private volatile boolean active;
	
	private UDPReceiver receiver;
	private UDPSender sender;
	
	private final UDPSession session;

	private UDPInputStream inputStream;
	private UDPOutputStream outputStream;

	public UDPSocket(UDPEndPoint endpoint, UDPSession session)throws SocketException,UnknownHostException{
		this.endpoint=endpoint;
		this.session=session;
		this.receiver=new UDPReceiver(session,endpoint);
		this.sender=new UDPSender(session,endpoint);
	}

	public void doWrite(byte[] data) throws IOException{
			DATSegment packet=new DATSegment();
			int seqNo=sender.getNextSequenceNumber();
			packet.seq(seqNo);
			packet.setSession(session);
			packet.setData(data);
			try {
				sender.sendUDPPacket(packet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public void close()throws IOException{
		active=false;
	}
	
	public synchronized UDPInputStream getInputStream()throws IOException{
		if(inputStream==null){
			inputStream=new UDPInputStream(this);
		}
		return inputStream;
	}
    
	public synchronized UDPOutputStream getOutputStream(){
		if(outputStream==null){
			outputStream=new UDPOutputStream(this);
		}
		return outputStream;
	}
	
	public UDPReceiver getReceiver() {
		return receiver;
	}

	public UDPSender getSender() {
		return sender;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public UDPEndPoint getEndpoint() {
		return endpoint;
	}
	
	public final UDPSession getSession(){
		return session;
	}

}
