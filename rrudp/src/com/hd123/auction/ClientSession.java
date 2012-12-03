
package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.ACKSegment;
import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;


public class ClientSession extends UDPSession {

	private static final Logger logger = Logger.getLogger(ClientSession.class.getName());
	public static final int DATAGRAM_SIZE=1400;
	private final DatagramSocket dgSocket;
	private final DatagramPacket dgPacket;
	protected UDPSocket socket;
	
	public ClientSession(DatagramSocket dgSocket, Destination dest)throws SocketException{
		super(dest);
		this.dgSocket=dgSocket;
		//每个目的地址独有的数据包缓冲区
		dgPacket = new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE,dest.getAddress(),dest.getPort());
	}
	
	public void received(Segment packet) {
		//响应的同步包
		if (packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;
			if(getState() == SYN_SENT){
				try{
					//发送第二次同步包 可靠
					sendConfirmation(syn.seq());
					setState(ESTABLISHED);
					socket.setInitialDataSequenceNumber(syn.seq()+1);
					synchronized (connectedSyn) {
						connectedSyn.notify();
					}
				}catch(Exception ex){
					logger.log(Level.WARNING,"Error creating socket",ex);
					setState(INVALID);
				}
			}
			else if(getState() == CLOSED){
				try{
					//服务器发送第一次同步包 非可靠
					sendHandShake(syn.seq());
					socket.setInitialDataSequenceNumber(syn.seq()+1);
					setState(SYN_RECEIVE);
				}catch(Exception ex){
					logger.log(Level.WARNING,"Error creating socket",ex);
					setState(INVALID);
				}
			}
			logger.info("Received :"+syn);
		}//end of syn
		socket.received(packet);
	}

	public UDPSocket getSocket() {
		return socket;
	}
	
	public void setSocket(UDPSocket socket) {
		this.socket = socket;
	}

	//第一次握手 服务器
	protected void sendHandShake(int seq) throws IOException{
		SYNSegment syn = new SYNSegment(incrementSequenceAndGet());
		syn.setAck(seq);
		syn.setSession(this);
		doSend(syn);
	}

	//客户端的二次确认
	protected void sendConfirmation(int seq) throws IOException{
		ACKSegment ack = new ACKSegment(incrementSequenceAndGet(),seq);
		ack.setSession(this);
		logger.info("Sending confirmation "+ack);
		doSend(ack);
	}
	
	//发送数据包
	protected void doSend(Segment packet) throws IOException{
			byte[] data=packet.getBytes();
			DatagramPacket dgp = packet.getSession().getDatagram();
			dgp.setData(data);
			dgSocket.send(dgp);
	}

}
