package com.hd123.auction;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class UDTServerSocket {
	
	private static final Logger logger=Logger.getLogger(UDTClient.class.getName());
	private final UDPEndPoint endpoint;
	private boolean started=false;
	private volatile boolean shutdown=false;
	
	public UDTServerSocket(InetAddress localAddress, int port)throws SocketException,UnknownHostException{
		endpoint=new UDPEndPoint(localAddress,port);
		logger.info("Created server endpoint on port "+endpoint.getLocalPort());
	}

	public UDTServerSocket(int port) throws SocketException,UnknownHostException{
		this(InetAddress.getLocalHost(),port);
	}
	
	public UDTServerSocket(String ip,int port) throws SocketException,UnknownHostException{
		this(InetAddress.getByName(ip),port);
	}
	
	public synchronized UDTSocket accept() throws InterruptedException{
		if(!started){
			endpoint.start(true);//启动服务端
			started=true;
		}
		while(!shutdown){
//			UDTSession session=endpoint.accept(10000, TimeUnit.MILLISECONDS);
			UDTSession session=endpoint.accept();
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
