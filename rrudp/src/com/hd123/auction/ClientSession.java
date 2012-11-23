
package com.hd123.auction;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;


/**
 * Client side of a client-server UDT connection. 
 * Once established, the session provides a valid {@link UDTSocket}.
 */
public class ClientSession extends UDTSession {

	private static final Logger logger=Logger.getLogger(ClientSession.class.getName());

	private UDPEndPoint endPoint;

	public ClientSession(UDPEndPoint endPoint, Destination dest)throws SocketException{
		super("ClientSession localPort="+endPoint.getLocalPort(),dest);
		this.endPoint=endPoint;
		logger.info("Created "+toString());
	}

	public void connect() throws InterruptedException,IOException{
		int n=0;
		while(getState()!=READY){
			//发送握手协议同步包
			sendHandShake();
			if(getState()==invalid)throw new IOException("Can't connect!");
			n++;
			if(getState()!=READY)Thread.sleep(500);
		}
//		cc.init();
		logger.info("Connected, "+n+" handshake packets sent");		
	}

	@Override
	public void received(Segment packet, Destination peer) {

		lastPacket=packet;

		//服务端响应的同步包
		if (packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;

			logger.info("Received connection handshake from "+peer+"\n"+syn);

			if (getState()!=READY) {
					try{
						//TODO 验证目的地址是否是当前session的地址
						//发送第二次同步包
						sendConfirmation(syn);
					}catch(Exception ex){
						logger.log(Level.WARNING,"Error creating socket",ex);
						setState(invalid);
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
				setState(invalid);
			}
			return;
		}
	}


	//handshake for connect
	protected void sendHandShake()throws IOException{
		SYNSegment syn = new SYNSegment(0);
		syn.setSession(this);
		endPoint.doSend(syn);
	}

	//2nd handshake for connect
	protected void sendConfirmation(SYNSegment hs)throws IOException{
		SYNSegment syn = new SYNSegment();
		syn.setSession(this);
		logger.info("Sending confirmation "+syn);
		endPoint.doSend(syn);
	}


}
