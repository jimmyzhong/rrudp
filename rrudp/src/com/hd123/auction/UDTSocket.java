
package com.hd123.auction;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class UDTSocket {
	
	private final UDPEndPoint endpoint;
	
	private volatile boolean active;
	
	private UDTReceiver receiver;
	private UDTSender sender;
	
	private final UDTSession session;

	private UDTInputStream inputStream;
	private UDTOutputStream outputStream;

	public UDTSocket(UDPEndPoint endpoint, UDTSession session)throws SocketException,UnknownHostException{
		this.endpoint=endpoint;
		this.session=session;
		this.receiver=new UDTReceiver(session,endpoint);
		this.sender=new UDTSender(session,endpoint);
	}
	
//	protected void doWrite(byte[]data)throws IOException{
//		doWrite(data, 0, data.length);
//	}
//	
//	protected void doWrite(byte[]data, int offset, int length)throws IOException{
//		try{
//			doWrite(data, offset, length, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
//		}catch(InterruptedException ie){
//			IOException io=new IOException();
//			io.initCause(ie);
//			throw io;
//		}
//	}
//	
//	protected void doWrite(byte[] data, int offset, int length, int timeout, TimeUnit units)throws IOException,InterruptedException{
//		ByteBuffer bb=ByteBuffer.wrap(data,offset,length);
//		long seqNo=0;
//		while(bb.remaining()>0){
//			int len=Math.min(bb.remaining(),chunksize);
//			byte[]chunk=new byte[len];
//			bb.get(chunk);
//			DataPacket packet=new DataPacket();
//			seqNo=sender.getNextSequenceNumber();
//			packet.setPacketSequenceNumber(seqNo);
//			packet.setSession(session);
//			packet.setDestinationID(session.getDestination().getSocketID());
//			packet.setData(chunk);
//			//put the packet into the send queue
//			while(!sender.sendUdtPacket(packet, timeout, units)){
//				throw new IOException("Queue full");
//			}
//		}
//		if(length>0)active=true;
//	}
	
	public void close()throws IOException{
		if(inputStream!=null)
			inputStream.close();
		if(outputStream!=null)
			outputStream.close();
		active=false;
	}
	
	public synchronized UDTInputStream getInputStream()throws IOException{
		if(inputStream==null){
			inputStream=new UDTInputStream(this);
		}
		return inputStream;
	}
    
	public synchronized UDTOutputStream getOutputStream(){
		if(outputStream==null){
			outputStream=new UDTOutputStream(this);
		}
		return outputStream;
	}
	
	public UDTReceiver getReceiver() {
		return receiver;
	}

	public UDTSender getSender() {
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
	
	public final UDTSession getSession(){
		return session;
	}

}
