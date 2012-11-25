
package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;


public class ClientSession extends UDPSession {

	private static final Logger logger = Logger.getLogger(ClientSession.class.getName());
	public static final int DATAGRAM_SIZE=1400;
	private final DatagramSocket dgSocket;
	private final DatagramPacket dgPacket;

	public ClientSession(DatagramSocket dgSocket, Destination dest)throws SocketException{
		super(dest);
		this.dgSocket=dgSocket;
		dgPacket = new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE,dest.getAddress(),dest.getPort());
	}
	
	@Override
	public void received(Segment packet) {
		//服务端响应的同步包
		if (packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;

			logger.info("Received connection handshake from "+dest+"\n"+syn);

			if (getState()!=READY) {
					try{
						//TODO 验证目的地址是否是当前session的地址
						//发送第二次同步包
						sendConfirmation(syn);
					}catch(Exception ex){
						logger.log(Level.WARNING,"Error creating socket",ex);
						setState(INVALID);
					}
					return;
			}
		}

		if(getState() == READY) {

//			if(packet instanceof Shutdown){
//				setState(shutdown);
//				active=false;
//				logger.info("Connection shutdown initiated by the other side.");
//				return;
//			}
			active = true;
			try{
//					socket.getSender().receive(lastPacket);
//					socket.getReceiver().receive(lastPacket);	
			}catch(Exception ex){
				logger.log(Level.SEVERE,"Error in "+toString(),ex);
				setState(INVALID);
			}
			return;
		}
	}


	//handshake for connect
	protected void sendHandShake()throws IOException{
		SYNSegment syn = new SYNSegment(0);
		syn.setSession(this);
		doSend(syn);
	}

	//2nd handshake for connect
	protected void sendConfirmation(SYNSegment hs)throws IOException{
		SYNSegment syn = new SYNSegment();
		syn.setSession(this);
		logger.info("Sending confirmation "+syn);
		doSend(syn);
	}
	
	//发送数据包
		protected void doSend(Segment packet) throws IOException{
			byte[] data=packet.getBytes();
			DatagramPacket dgp = packet.getSession().getDatagram();
			dgp.setData(data);
			dgSocket.send(dgp);
		}


}
