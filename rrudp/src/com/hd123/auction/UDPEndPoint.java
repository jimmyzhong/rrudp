
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
 * dispatching them to the correct {@link UDTSession}
 */
public class UDPEndPoint {

	private static final Logger logger=Logger.getLogger(UDPEndPoint.class.getName());

	private final int port;

	private final DatagramSocket dgSocket;

	//以IP地址和端口号作为key
	private final Map<Destination,UDTSession>sessions=new ConcurrentHashMap<Destination, UDTSession>();

	//last received packet
	private Segment lastPacket;

	//if the endpoint is configured for a server socket,
	//this queue is used to handoff new UDTSessions to the application
	private final SynchronousQueue<UDTSession> sessionHandoff=new SynchronousQueue<UDTSession>();
	
	private boolean serverSocketMode=false;

	private volatile boolean stopped=false;

	public static final int DATAGRAM_SIZE=1400;

	/**
	 * create an endpoint on the given socket
	 * 
	 * @param socket -  a UDP datagram socket
	 */
	public UDPEndPoint(DatagramSocket socket){
		this.dgSocket=socket;
		port=dgSocket.getLocalPort();
	}
	
	/**
	 * bind to any local port on the given host address
	 * @param localAddress
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint(InetAddress localAddress)throws SocketException, UnknownHostException{
		this(localAddress,0);
	}

	/**
	 * Bind to the given address and port
	 * @param localAddress
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint(InetAddress localAddress, int localPort)throws SocketException, UnknownHostException{
		if(localAddress==null){
			dgSocket=new DatagramSocket(localPort, localAddress);
		}else{
			dgSocket=new DatagramSocket(localPort);
		}
		if(localPort>0)this.port = localPort;
		else port=dgSocket.getLocalPort();
		
		//set a time out to avoid blocking in doReceive()
		dgSocket.setSoTimeout(100000);
		//buffer size
		dgSocket.setReceiveBufferSize(128*1024);
	}

	/**
	 * bind to the default network interface on the machine
	 * 
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
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

	/**
	 * start the endpoint. If the serverSocketModeEnabled flag is <code>true</code>,
	 * a new connection can be handed off to an application. The application needs to
	 * call #accept() to get the socket
	 * @param serverSocketModeEnabled
	 */
	public void start(boolean serverSocketModeEnabled){
		serverSocketMode=serverSocketModeEnabled;
		//start receive thread
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

	/**
	 * @return the port which this client is bound to
	 */
	public int getLocalPort() {
		return this.dgSocket.getLocalPort();
	}
	/**
	 * @return Gets the local address to which the socket is bound
	 */
	public InetAddress getLocalAddress(){
		return this.dgSocket.getLocalAddress();
	}

	DatagramSocket getSocket(){
		return dgSocket;
	}

	public void addSession(Destination destinationID,UDTSession session){
		logger.info("Storing session <"+destinationID+">");
		sessions.put(destinationID, session);
	}

	public UDTSession getSession(Long destinationID){
		return sessions.get(destinationID);
	}

	public Collection<UDTSession> getSessions(){
		return sessions.values();
	}

	/**
	 * wait the given time for a new connection
	 * @param timeout - the time to wait
	 * @param unit - the {@link TimeUnit}
	 * @return a new {@link UDTSession}
	 * @throws InterruptedException
	 */
	protected UDTSession accept(long timeout, TimeUnit unit)throws InterruptedException{
		return sessionHandoff.poll(timeout, unit);
	}


	final DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);

	
	private UDTSession lastSession;
	
	//MeanValue v=new MeanValue("receiver processing ",true, 256);
	
	private int n=0;
	
	private final Object lock=new Object();
	
	protected void doReceive() throws IOException{
		while(!stopped){
			try{
				try{
					dgSocket.receive(dp);
					Destination peer=new Destination(dp.getAddress(), dp.getPort());
					int l=dp.getLength();
					Segment seg = Segment.parse(dp.getData());
					lastPacket=seg;

					//handle connection handshake 
					if(seg instanceof SYNSegment){
						synchronized(lock){
							UDTSession session=sessions.get(peer);
							if(session==null){
								session=new ServerSession(dp,this);
								addSession(peer,session);
								//TODO need to check peer to avoid duplicate server session
								if(serverSocketMode){
									logger.fine("Pooling new request.");
									sessionHandoff.put(session);
									logger.fine("Request taken for processing.");
								}
							}
							session.received(seg,peer);
						}
					}
					else{
						//dispatch to existing session
						UDTSession session;
						if(peer.equals(lastDestID)){
							session=lastSession;
						}
						else{
							session=sessions.get(peer);
							lastSession=session;
						}
						if(session==null){
							n++;
							if(n%100==1){
								logger.warning("Unknown session  requested from <"+peer+"> packet type "+seg.getClass().getName());
							}
						}
						else{
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

	protected void doSend(Segment packet) throws IOException{
		byte[] data=packet.getBytes();
		DatagramPacket dgp = packet.getSession().getDatagram();
		dgp.setData(data);
		dgSocket.send(dgp);
	}

	public String toString(){
		return  "UDPEndpoint port="+port;
	}

	public void sendRaw(DatagramPacket p)throws IOException{
		dgSocket.send(p);
	}
}
