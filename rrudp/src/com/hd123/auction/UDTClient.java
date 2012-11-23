
package com.hd123.auction;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDTClient {

	private static final Logger logger=Logger.getLogger(UDTClient.class.getName());
	private final UDPEndPoint clientEndpoint;
	private ClientSession clientSession;


	public UDTClient(InetAddress address, int localport)throws SocketException, UnknownHostException{
		clientEndpoint=new UDPEndPoint(address,localport);
		logger.info("Created client endpoint on port "+localport);
	}

	public UDTClient(InetAddress address)throws SocketException, UnknownHostException{
		clientEndpoint=new UDPEndPoint(address);
		logger.info("Created client endpoint on port "+clientEndpoint.getLocalPort());
	}

	public UDTClient(UDPEndPoint endpoint) throws SocketException, UnknownHostException{
		clientEndpoint=endpoint;
	}

	public void connect(String host, int port) throws InterruptedException, UnknownHostException, IOException{
		InetAddress address=InetAddress.getByName(host);
		Destination destination=new Destination(address,port);
		//create client session...
		clientSession=new ClientSession(clientEndpoint,destination);
		clientEndpoint.addSession(clientSession.getSocketID(), clientSession);

		clientEndpoint.start();
		clientSession.connect();
		//wait for handshake
		while(!clientSession.isReady()){
			Thread.sleep(500);
		}
		logger.info("The UDPClient is connected");
		Thread.sleep(500);
	}

	/**
	 * sends the given data asynchronously
	 * 
	 * @param data
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void send(byte[]data)throws IOException, InterruptedException{
		clientSession.getSocket().doWrite(data);
	}

	public void sendBlocking(byte[]data)throws IOException, InterruptedException{
		clientSession.getSocket().doWriteBlocking(data);
	}

	public int read(byte[]data)throws IOException, InterruptedException{
		return clientSession.getSocket().getInputStream().read(data);
	}

	public void flush()throws IOException, InterruptedException{
		clientSession.getSocket().flush();
	}


	public void shutdown() throws IOException{

		if (clientSession.isReady()&& clientSession.active==true) 
		{
			Shutdown shutdown = new Shutdown();
			shutdown.setDestinationID(clientSession.getDestination().getSocketID());
			shutdown.setSession(clientSession);
			try{
				clientEndpoint.doSend(shutdown);
			}
			catch(IOException e)
			{
				logger.log(Level.SEVERE,"ERROR: Connection could not be stopped!",e);
			}
			clientSession.getSocket().getReceiver().stop();
			clientEndpoint.stop();
		}
	}

	public UDTInputStream getInputStream()throws IOException{
		return clientSession.getSocket().getInputStream();
	}

	public UDTOutputStream getOutputStream()throws IOException{
		return clientSession.getSocket().getOutputStream();
	}

	public UDPEndPoint getEndpoint()throws IOException{
		return clientEndpoint;
	}

}
