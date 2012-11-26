package com.hd123.auction;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class ReliableServerSocket {

	private static final Logger logger = Logger.getLogger(ReliableSocket.class
			.getName());
	private boolean started = false;
	private volatile boolean shutdown = false;

	private ServerSocketImpl impl;

	public ReliableServerSocket(String host, int port) throws SocketException,
			UnknownHostException {
		impl = new ServerSocketImpl(host, port);
		logger.info("Created server endpoint on port " + impl.getLocalPort());
	}

	public synchronized ReliableSocket accept() throws InterruptedException {
		if (!started) {
			started = true;
		}
		while (!shutdown) {
			return impl.accept();
		}
		return null;
	}

	public void shutDown() {
		shutdown = true;
		impl.stop();
	}

}
