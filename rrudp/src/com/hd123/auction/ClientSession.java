
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
		//每个目的地址独有的数据包缓冲区
		dgPacket = new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE,dest.getAddress(),dest.getPort());
	}
	
	@Override
	public void received(Segment packet) {
		//响应的同步包
		if (packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;
			if(getState() == SYN_SENT){
				try{
					//发送第二次同步包 可靠
					sendConfirmation(syn);
					setState(ESTABLISHED);
				}catch(Exception ex){
					logger.log(Level.WARNING,"Error creating socket",ex);
					setState(INVALID);
				}
			}
			else if(getState() == CLOSED){
				try{
					//服务器发送第一次同步包 非可靠
					sendHandShake();
					setState(SYN_SENT);
				}catch(Exception ex){
					logger.log(Level.WARNING,"Error creating socket",ex);
					setState(INVALID);
				}
			}
			logger.info("Received connection handshake from "+dest+"\n"+syn);
			logger.info("Received :"+syn);
		}//end of syn

		if(getState() == ESTABLISHED) {
			//判断关闭
			return;
		}
		socket.received(packet);
	}

	//第一次握手
	protected void sendHandShake() throws IOException{
		SYNSegment syn = new SYNSegment(0);
		syn.setSession(this);
		doSend(syn);
	}

	//客户端的二次确认
	protected void sendConfirmation(SYNSegment syn) throws IOException{
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
