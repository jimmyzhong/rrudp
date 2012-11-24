
package com.hd123.auction;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReliableSocket {

	private static final Logger logger=Logger.getLogger(ReliableSocket.class.getName());
	
	private final UDPEndPoint clientEndpoint;
	private ClientSession clientSession;


	public ReliableSocket(InetAddress address, int localport)throws SocketException, UnknownHostException{
		clientEndpoint=new UDPEndPoint(address,localport);
		logger.info("Created client endpoint on port "+localport);
	}

	public ReliableSocket(InetAddress address)throws SocketException, UnknownHostException{
		clientEndpoint=new UDPEndPoint(address);
		logger.info("Created client endpoint on port "+clientEndpoint.getLocalPort());
	}

	public void connect(String host, int port) throws InterruptedException, UnknownHostException, IOException{
		InetAddress address=InetAddress.getByName(host);
		//服务器地址
		Destination destination=new Destination(address,port);
		//create client session...
		clientSession=new ClientSession(clientEndpoint,destination);
		clientEndpoint.addSession(destination, clientSession);
		clientEndpoint.start();
		clientSession.connect();
		
		while(!clientSession.isReady()){
			Thread.sleep(500);
		}
		logger.info("The UDPClient is connected");
		Thread.sleep(500);
	}

	public void shutdown() throws IOException{

			clientSession.getSocket().getReceiver().stop();
			clientSession.getSocket().getSender().stop();
			clientEndpoint.stop();
	}

	public UDPInputStream getInputStream()throws IOException{
		return clientSession.getSocket().getInputStream();
	}

	public UDPOutputStream getOutputStream()throws IOException{
		return clientSession.getSocket().getOutputStream();
	}

	public UDPEndPoint getEndpoint()throws IOException{
		return clientEndpoint;
	}

}
