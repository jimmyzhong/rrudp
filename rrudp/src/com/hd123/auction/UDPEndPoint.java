
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

import com.hd123.auction.seg.SYNSegment;
import com.hd123.auction.seg.Segment;
import com.hd123.auction.util.UDTThreadFactory;

/**
 * the UDPEndpoint takes care of sending and receiving UDP network packets,
 * dispatching them to the correct {@link UDPSession}
 */
public class UDPEndPoint {

	private static final Logger logger=Logger.getLogger(UDPEndPoint.class.getName());

	private final int port;

	private final DatagramSocket dgSocket;

	//以IP地址和端口号作为key
	private final Map<Destination,UDPSession>sessions=new ConcurrentHashMap<Destination, UDPSession>();

	private  UDPSession clientSession;
	
	//last received packet
	private Segment lastPacket;
	private Destination lastDestination;

	//if the endpoint is configured for a server socket,
	//this queue is used to handoff new UDTSessions to the application
	private final SynchronousQueue<UDPSession> sessionHandoff=new SynchronousQueue<UDPSession>();
	
	private boolean serverSocketMode=false;

	private volatile boolean stopped=false;

	public static final int DATAGRAM_SIZE=1400;

	public UDPEndPoint(DatagramSocket socket){
		this.dgSocket=socket;
		port=dgSocket.getLocalPort();
	}
	
	public UDPEndPoint(InetAddress localAddress)throws SocketException, UnknownHostException{
		this(localAddress,0);
	}
	
	public UDPEndPoint(InetAddress localAddress, int localPort)throws SocketException, UnknownHostException{
		if(localAddress==null){
			dgSocket=new DatagramSocket(localPort, localAddress);
		}else{
			dgSocket=new DatagramSocket(localPort);
		}
		if(localPort>0)this.port = localPort;
		else port=dgSocket.getLocalPort();
		
		dgSocket.setSoTimeout(100000);
		dgSocket.setReceiveBufferSize(128*1024);
	}

	public UDPEndPoint(int localPort)throws SocketException, UnknownHostException{
		this(null,localPort);
	}

	/**
	 * bind to an ephemeral port on the default network interface on the machine
	 * 
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint()throws SocketException, UnknownHostException{
		this(null,0);
	}

	public void start(boolean serverSocketModeEnabled){
		serverSocketMode=serverSocketModeEnabled;
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

	//作为client启动
	public void start(){
		start(false);
	}

	public void stop(){
		stopped=true;
		dgSocket.close();
	}
	
	public void addSession(Destination destinationID,UDPSession session){
		logger.info("Storing session <"+destinationID+">");
		sessions.put(destinationID, session);
	}

	public Collection<UDPSession> getSessions(){
		return sessions.values();
	}

	protected UDPSession accept() throws InterruptedException{
		return sessionHandoff.take();
	}
	
	protected UDPSession accept(long timeout, TimeUnit unit)throws InterruptedException{
		return sessionHandoff.poll(timeout, unit);
	}

	final DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);
	
	private UDPSession lastSession;
	
	private final Object lock=new Object();
	
	protected void doReceive() throws IOException{
		while(!stopped){
			try{
				try{
					dgSocket.receive(dp);
					Destination peer=new Destination(dp.getAddress(), dp.getPort());
					Segment seg = Segment.parse(dp.getData());
					lastPacket=seg;
					if(seg instanceof SYNSegment){
						UDPSession session;
						//服务器模式
						if(serverSocketMode){
							synchronized(lock){
							session=sessions.get(peer);
							if(session==null){
								session=new ServerSession(dp,this);
								addSession(peer,session);
									logger.fine("Pooling new request.");
									sessionHandoff.put(session);
									logger.fine("Request taken for processing.");
								}
							}
						}else{//client
							synchronized(lock){
								session=new ClientSession(this,peer);
								lastSession=session;
							}
						}
						session.received(seg,peer);
					}
					else{
						UDPSession session;
						if(peer.equals(lastDestination)){
							session=lastSession;
						}
						else{
							session=sessions.get(peer);
							lastSession=session;
						}
						if(session!=null){
							session.received(seg,peer);
						}
					}
				}catch(SocketException ex){
					logger.log(Level.INFO, "SocketException: "+ex.getMessage());
				}catch(SocketTimeoutException ste){
					//can safely ignore... we will retry until the endpoint is stopped
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
