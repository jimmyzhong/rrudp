package com.hd123.auction;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class ReliableServerSocket {
	
	private static final Logger logger=Logger.getLogger(ReliableSocket.class.getName());
	private final UDPEndPoint endpoint;
	private boolean started=false;
	private volatile boolean shutdown=false;
	
	public ReliableServerSocket(InetAddress localAddress, int port)throws SocketException,UnknownHostException{
		endpoint=new UDPEndPoint(localAddress,port);
		logger.info("Created server endpoint on port "+endpoint.getLocalPort());
	}

	public ReliableServerSocket(int port) throws SocketException,UnknownHostException{
		this(InetAddress.getLocalHost(),port);
	}
	
	public ReliableServerSocket(String ip,int port) throws SocketException,UnknownHostException{
		this(InetAddress.getByName(ip),port);
	}
	
	public synchronized UDPSocket accept() throws InterruptedException{
		if(!started){
			endpoint.start(true);//启动服务端
			started=true;
		}
		while(!shutdown){
//			UDTSession session=endpoint.accept(10000, TimeUnit.MILLISECONDS);
			UDPSession session=endpoint.accept();
			return session.getSocket();
		}
		throw new InterruptedException();
	} 
	
	public void shutDown(){
		shutdown=true;
		endpoint.stop();
	}
	
	public UDPEndPoint getEndpoint(){
		return endpoint;
	}
}
