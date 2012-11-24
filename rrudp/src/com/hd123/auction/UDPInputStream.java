package com.hd123.auction;

import java.io.IOException;

import com.hd123.auction.util.ReceiveBuffer;

public class UDPInputStream {

	private final UDPSocket socket;

	private final ReceiveBuffer receiveBuffer;

	private volatile boolean closed = false;

	public UDPInputStream(UDPSocket socket) throws IOException {
		this.socket = socket;
		int capacity =  128;
		int initialSequenceNum = socket.getSession().getInitialSequenceNumber();
		receiveBuffer = new ReceiveBuffer(capacity, socket.getSession()
				.getSequenceSize(), initialSequenceNum);
	}


	public byte[] read() throws IOException {
		try {
			AppData data = receiveBuffer.poll();
			return data.getData();
		} catch (Exception ex) {
			IOException e = new IOException();
			e.initCause(ex);
			throw e;
		}
	}

	protected boolean haveNewData(int seq, byte[] data) throws IOException {
		return receiveBuffer.offer(new AppData(seq, data));
	}

	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
	}

	public int getReceiveBufferSize() {
		return receiveBuffer.getSize();
	}

	public static class AppData implements Comparable<AppData> {
		private final int sequenceNumber;
		private final byte[] data;

		public AppData(int sequenceNumber, byte[] data) {
			this.sequenceNumber = sequenceNumber;
			this.data = data;
		}

		public int compareTo(AppData o) {
			return (int) (sequenceNumber - o.sequenceNumber);
		}

		public String toString() {
			return sequenceNumber + "[" + data.length + "]";
		}

		public int getSequenceNumber() {
			return sequenceNumber;
		}

		public byte[] getData() {
			return data;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (int) (sequenceNumber ^ (sequenceNumber >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AppData other = (AppData) obj;
			if (sequenceNumber != other.sequenceNumber)
				return false;
			return true;
		}

	}

}
