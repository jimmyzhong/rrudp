package com.hd123.auction;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReliableSocket {

	private static final Logger logger = Logger.getLogger(ReliableSocket.class
			.getName());

	private ClientSocketImpl sockImpl;
	private UDPSession session;
	private boolean isClientMode = true;
	private String localHost;
	private int localPort;
	private String remoteHost;
	private int remotePort;

	public ReliableSocket() {
	}

	public ReliableSocket(String host, int port) throws SocketException,
			UnknownHostException {
		localHost = host;
		localPort = port;
		logger.info("Created client endpoint on port " + port);
	}

	public ReliableSocket(UDPSession session) throws SocketException,
			UnknownHostException {
		this.session = session;
	}

	public void connect(String host, int port) throws InterruptedException,
			UnknownHostException, IOException {
		remoteHost = host;
		remotePort = port;
		InetAddress address = InetAddress.getByName(host);
		// 服务器地址
		Destination destination = new Destination(address, port);

		sockImpl = new ClientSocketImpl(host, port);
		session = new ClientSession(new DatagramSocket(port,address), destination);
		sockImpl.setSession(session);

		sockImpl.connect(host, port);

		while (!session.isReady()) {
			Thread.sleep(500);
		}
		logger.info("The UDPClient is connected");
		Thread.sleep(500);
	}

	public void shutdown() throws IOException {

		session.getSocket().getReceiver().stop();
		session.getSocket().getSender().stop();
		if (sockImpl != null)
			sockImpl.stop();
	}

	public UDPInputStream getInputStream() throws IOException {
		return session.getSocket().getInputStream();
	}

	public UDPOutputStream getOutputStream() throws IOException {
		return session.getSocket().getOutputStream();
	}

}
