package com.hd123.auction.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.hd123.auction.ReliableServerSocket;
import com.hd123.auction.ReliableSocket;
import com.hd123.auction.UDPOutputStream;

public class ServerTest {

	public static void main(String[] args) throws InterruptedException,
			IOException {
		String serverHost = "192.168.111.10";
		int serverPort = 7000;
		String fileName = "c:\\sendfile.txt";

		ReliableServerSocket socket = new ReliableServerSocket(serverHost,
				serverPort);

		ReliableSocket client = socket.accept();

		UDPOutputStream os = client.getOutputStream();
		BufferedReader is = new BufferedReader(new FileReader(fileName));
		String x;
		while ((x = is.readLine()) != null) {
			os.write(x.getBytes());
			Thread.sleep(5000);
		}
		Thread.sleep(5000000);
		client.shutdown();

	}

}
