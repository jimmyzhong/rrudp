package com.hd123.auction;

public class ClientDef {

	private ReliableSocket socket;

	private ClientSession session;
	
	public ClientSession getSession() {
		return session;
	}

	public void setSession(ClientSession session) {
		this.session = session;
	}

	public ReliableSocket getSocket() {
		return socket;
	}

	public void setSocket(ReliableSocket socket) {
		this.socket = socket;
	}
}
