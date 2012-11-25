
package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.DATSegment;
import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.UDTThreadFactory;

public class ServerSocketImpl {

	private static final Logger logger=Logger.getLogger(ServerSocketImpl.class.getName());

	private final int port;

	//一个服务器对应一个socket
	private final DatagramSocket dgSocket;
	final DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);

	private final Map<Destination,ClientDef> clients = new ConcurrentHashMap<Destination, ClientDef>();

	private final SynchronousQueue<ReliableSocket> sessionHandoff=new SynchronousQueue<ReliableSocket>();
	
	private volatile boolean stopped=false;

	public static final int DATAGRAM_SIZE=1400;

	public ServerSocketImpl(String localHost, int localPort)throws SocketException, UnknownHostException{
		dgSocket=new DatagramSocket(localPort,InetAddress.getByName(localHost));
		this.port = localPort;
	}

	public void start(){
		Runnable receive=new Runnable(){
			public void run(){
				try{
					doReceive();
				}catch(Exception ex){
					logger.log(Level.WARNING,"",ex);
				}
			}
		};
		Thread t=UDTThreadFactory.get().newThread(receive);
		t.setDaemon(true);
		t.start();
		logger.info("UDTEndpoint started.");
	}

	public void stop(){
		stopped=true;
		dgSocket.close();
	}
	
	public void addClient(Destination dest, ClientDef client){
		logger.info("adding client <"+dest+">");
		clients.put(dest, client);
	}

	protected ReliableSocket accept() throws InterruptedException{
		return sessionHandoff.take();
	}
	
	protected ReliableSocket accept(long timeout, TimeUnit unit)throws InterruptedException{
		return sessionHandoff.poll(timeout, unit);
	}
	
	protected void doReceive() throws IOException{
		while(!stopped){
			try{
				try{
					dgSocket.receive(dp);
					Destination peer=new Destination(dp.getAddress(), dp.getPort());
					Segment seg = Segment.parse(dp.getData());
					if(seg instanceof DATSegment){
						ClientDef client = clients.get(peer);
						if(client == null)
							continue;
						client.getSession().received(seg);
					}
					if(seg instanceof SYNSegment){
						ClientDef client = new ClientDef();
						ReliableSocket clientSocket = new ReliableSocket();
						client.setSocket(clientSocket);
						UDPSession session = new ClientSession(dgSocket,peer);
						client.setSession(session);
						addClient(peer, client);
						sessionHandoff.put(clientSocket);
						session.received(seg);
					}
				}catch(SocketException ex){
					logger.log(Level.INFO, "SocketException: "+ex.getMessage());
				}catch(SocketTimeoutException ste){
					//
				}

			}catch(Exception ex){
				logger.log(Level.WARNING, "Got: "+ex.getMessage(),ex);
			}
		}
	}

	//发送数据包
	protected void doSend(Segment packet) throws IOException{
		byte[] data=packet.getBytes();
		DatagramPacket dgp = packet.getSession().getDatagram();
		dgp.setData(data);
		dgSocket.send(dgp);
	}

	public void sendRaw(DatagramPacket p)throws IOException{
		dgSocket.send(p);
	}
	
	public int getLocalPort() {
		return this.dgSocket.getLocalPort();
	}

	public InetAddress getLocalAddress(){
		return this.dgSocket.getLocalAddress();
	}

	protected DatagramSocket getSocket(){
		return dgSocket;
	}

	public String toString(){
		return  "UDPEndpoint port="+port;
	}
}
