
package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hd123.auction.seg.DATSegment;
import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.UDPThreadFactory;

public class ClientSocketImpl {

	private static final Logger logger=Logger.getLogger(ClientSocketImpl.class.getName());

	private final int port;
	private volatile boolean stopped=false;
	private final DatagramSocket dgSocket;
	private ClientSession session;
	
	private DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);
	
	public static final int DATAGRAM_SIZE=1400;

	public ClientSocketImpl(String host, int port)throws SocketException, UnknownHostException{
		dgSocket=new DatagramSocket(port,InetAddress.getByName(host));
		dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE,InetAddress.getByName(host),port);
		this.port = port;
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
		Thread t=UDPThreadFactory.get().newThread(receive);
		t.setDaemon(true);
		t.start();
		logger.info("ClientSocketImpl started.");
	}
	
	protected void doReceive() throws IOException{
		while(!stopped){
			try{
					dgSocket.receive(dp);
					Destination peer=new Destination(dp.getAddress(), dp.getPort());
					Segment seg = Segment.parse(dp.getData());
					if(seg instanceof DATSegment){
						session.received(seg);
					}
					if(seg instanceof SYNSegment){
						session.received(seg);
					}
			}catch(Exception ex){
				logger.log(Level.WARNING, "Got: "+ex.getMessage(),ex);
			}
		}
	}

	public void stop(){
		stopped = true;
		dgSocket.close();
	}
	
	public void setSession(ClientSession session){
		this.session = session;
	}

	public void connect(String remote, int port) throws InterruptedException, UnknownHostException, IOException{
		int n=0;
		while(true){
			//发送握手协议同步包
			sendHandShake();
			n++;
			if(session.getState()!=UDPSession.ESTABLISHED)
				Thread.sleep(5000);
			if(session.getState()==UDPSession.ESTABLISHED)
				break;
			if(n == 8)
				throw new RuntimeException("time out");
		}
		logger.info("Connected, "+n+" handshake packets sent");		
	}
	
	//发送握手协议
		protected void sendHandShake()throws IOException{
			logger.info("first sendHandShake");		
			session.setState(UDPSession.SYN_SENT);
			SYNSegment syn = new SYNSegment(session.getSequence());
			syn.setSession(session);
			doSend(syn);
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
		return  "ClientSocketImpl port="+port;
	}
}
