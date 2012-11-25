package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;


public class ServerSession extends UDPSession {

	private static final Logger logger=Logger.getLogger(ServerSession.class.getName());

	private final ServerSocketImpl impl;

	public ServerSession(ServerSocketImpl impl, Destination peer) throws SocketException,UnknownHostException{
		super(peer);
		this.impl=impl;
	}

	@Override
	public void received(Segment packet){
		if(packet instanceof SYNSegment) {
			SYNSegment syn=(SYNSegment)packet;
			logger.info("Received :"+syn);

			if (getState()<=READY){
				if(getState()<=HANDSHARKING){
					setState(HANDSHARKING);
				}
				try{
					handleHandShake(syn);
					try{
						setState(READY);
						socket=new UDPSocket(impl, this);
					}catch(Exception e){
						logger.log(Level.SEVERE,"",e);
						setState(INVALID);
					}
				}catch(IOException ex){
					logger.log(Level.WARNING,"Error processing ConnectionHandshake",ex);
					setState(INVALID);
				}
				return;
			}
		}
		if(getState()== READY)
			return;
	}

	Segment getLastPacket(){
		return lastPacket;
	}
	protected void handleHandShake(Segment handshake)throws IOException{
		SYNSegment syn = new SYNSegment();
		logger.info("Sending reply "+syn);
		impl.doSend(syn);
	}





}

