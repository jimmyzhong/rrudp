package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;


public class ServerSession extends UDTSession {

	private static final Logger logger=Logger.getLogger(ServerSession.class.getName());

	private final UDPEndPoint endPoint;

	//last received packet (for testing purposes)
	private Segment lastPacket;

	public ServerSession(DatagramPacket dp, UDPEndPoint endPoint)throws SocketException,UnknownHostException{
		super("ServerSession localPort="+endPoint.getLocalPort()+" peer="+dp.getAddress()+":"+dp.getPort(),new Destination(dp.getAddress(),dp.getPort()));
		this.endPoint=endPoint;
		logger.info("Created "+toString()+" talking to "+dp.getAddress()+":"+dp.getPort());
	}

	int n_handshake=0;

	@Override
	public void received(Segment packet, Destination peer){
		lastPacket=packet;

		if(packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;
			logger.info("Received :"+syn);

			if (getState()<=ready){

				if(getState()<=handshaking){
					setState(handshaking);
				}
				try{
					handleHandShake(syn);
					n_handshake++;
					try{
						setState(ready);
						socket=new UDTSocket(endPoint, this);
					}catch(Exception e){
						logger.log(Level.SEVERE,"",e);
						setState(invalid);
					}
				}catch(IOException ex){
					//session invalid
					logger.log(Level.WARNING,"Error processing ConnectionHandshake",ex);
					setState(invalid);
				}
				return;
			}

		}
//		else if(packet instanceof KeepAlive) {
//			socket.getReceiver().resetEXPTimer();
//			active = true;
//			return;
//		}

		if(getState()== ready) {
			active = true;

//			if (packet instanceof KeepAlive) {
//				//nothing to do here
//				return;
//			}else if (packet instanceof Shutdown) {
//				try{
//					socket.getReceiver().stop();
//				}catch(IOException ex){
//					logger.log(Level.WARNING,"",ex);
//				}
//				setState(shutdown);
//				System.out.println("SHUTDOWN ***");
//				active = false;
//				logger.info("Connection shutdown initiated by the other side.");
//				return;
			}

			else{
				try{
//						socket.getSender().receive(packet);
//						socket.getReceiver().receive(packet);	
				}catch(Exception ex){
					//session invalid
					logger.log(Level.SEVERE,"",ex);
					setState(invalid);
				}
			}
			return;

	}

	Segment getLastPacket(){
		return lastPacket;
	}
	protected void handleHandShake(Segment handshake)throws IOException{
		SYNSegment syn = new SYNSegment();
		logger.info("Sending reply "+syn);
		endPoint.doSend(syn);
	}




}

