
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

public class ClientSocketImpl {

	private static final Logger logger=Logger.getLogger(ClientSocketImpl.class.getName());

	private final int port;

	private final DatagramSocket dgSocket;
	private UDPSession session;
	
	final DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);
	
	public static final int DATAGRAM_SIZE=1400;

	public ClientSocketImpl(String host, int port)throws SocketException, UnknownHostException{
		dgSocket=new DatagramSocket(port,InetAddress.getByName(host));
		this.port = port;
	}

	public void stop(){
		dgSocket.close();
	}
	
	public void setSession(UDPSession session){
		this.session = session;
	}

	public void connect(String remote, int port) throws InterruptedException, UnknownHostException, IOException{
		int n=0;
		while(session.getState()!=UDPSession.READY){
			//发送握手协议同步包
			sendHandShake();
			if(session.getState()==UDPSession.INVALID)
				throw new IOException("Can't connect!");
			n++;
			if(session.getState()!=UDPSession.READY)
				Thread.sleep(500);
		}
//		cc.init();
		logger.info("Connected, "+n+" handshake packets sent");		
	}
	
	//handshake for connect
		protected void sendHandShake()throws IOException{
			SYNSegment syn = new SYNSegment(0);
			syn.setSession(session);
			doSend(syn);
		}

		//2nd handshake for connect
		protected void sendConfirmation(SYNSegment hs)throws IOException{
			SYNSegment syn = new SYNSegment();
			syn.setSession(session);
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
