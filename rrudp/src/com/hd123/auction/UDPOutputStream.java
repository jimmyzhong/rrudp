package com.hd123.auction;
import java.io.IOException;

public class UDPOutputStream{

	private final UDPSocket socket;
	
	public UDPOutputStream(UDPSocket socket){
		this.socket=socket;	
	}
	
	public void write(byte[] b) throws IOException {
		socket.doWrite(b);
	}
}
