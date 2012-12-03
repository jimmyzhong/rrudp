package com.hd123.auction.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.hd123.auction.ReliableSocket;
import com.hd123.auction.UDPInputStream;

public class ClientTest2 {

	public static void main(String[] args) throws InterruptedException, IOException {
		String localHost = "192.168.111.1";
		int localPort = 7001;
		String serverHost = "192.168.111.1";
		int serverPort = 6001;
		String fileName = "c:\\receivefile";

		ReliableSocket socket = new ReliableSocket(localHost, localPort);
		socket.connect(serverHost, serverPort);

		UDPInputStream in = socket.getInputStream();
		OutputStream io = new FileOutputStream(fileName);
		byte[] data = null;
		do {
			data = in.read();
			if (data != null) {
				io.write(data);
			}
		} while (data != null);

	}

}
