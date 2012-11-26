package com.hd123.auction;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class ReliableSocket {

	private static final Logger logger = Logger.getLogger(ReliableSocket.class.getName());

	private ClientSocketImpl sockImpl;
	private ClientSession session;
	private boolean isClientMode = true;
	private String localHost;
	private int localPort;
	private String remoteHost;
	private int remotePort;

	public ReliableSocket(String host, int port) throws SocketException, UnknownHostException {
		localHost = host;
		localPort = port;
		logger.info("Created client endpoint on port " + port);
	}

	public ReliableSocket(ClientSession session) throws SocketException, UnknownHostException {
		this.session = session;
	}

	public void connect(String host, int port) throws InterruptedException, UnknownHostException, IOException {
		remoteHost = host;
		remotePort = port;
		InetAddress address = InetAddress.getByName(host);
		// 服务器地址
		Destination destination = new Destination(address, port);

		sockImpl = new ClientSocketImpl(localHost, localPort);
		session = new ClientSession(sockImpl.getSocket(), destination);
		sockImpl.setSession(session);

		sockImpl.connect(host, port);

		while (!session.isReady()) {
			Thread.sleep(500);
		}
		logger.info("The UDPClient is connected");
		Thread.sleep(500);
	}

	public void shutdown() throws IOException {
		session.getSocket().stop();
		if (sockImpl != null)
			sockImpl.stop();
	}

	public UDPInputStream getInputStream() throws IOException {
		if (session == null)
			throw new IOException("session is null");
		if (session.getSocket() == null)
			throw new IOException("socket is null");
		return session.getSocket().getInputStream();
	}

	public UDPOutputStream getOutputStream() throws IOException {
		if (session == null)
			throw new IOException("session is null");
		if (session.getSocket() == null)
			throw new IOException("socket is null");
		return session.getSocket().getOutputStream();
	}

}
