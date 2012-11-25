package com.hd123.auction;

public class ClientDef {

	private ReliableSocket socket;

	private UDPSession session;
	
	public UDPSession getSession() {
		return session;
	}

	public void setSession(UDPSession session) {
		this.session = session;
	}

	public ReliableSocket getSocket() {
		return socket;
	}

	public void setSocket(ReliableSocket socket) {
		this.socket = socket;
	}
}
