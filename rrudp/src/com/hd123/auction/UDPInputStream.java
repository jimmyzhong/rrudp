package com.hd123.auction;

import java.io.IOException;

import com.hd123.auction.seg.Segment;

public class UDPInputStream {

	private final UDPSocket socket;

	private volatile boolean closed = false;

	public UDPInputStream(UDPSocket socket) throws IOException {
		this.socket = socket;
	}

	public byte[] read() throws IOException {
		AppData data = socket.getReceiverBuffer().poll();
		byte[] src = data.getData();
		int length = src.length - Segment.RUDP_HEADER_LEN;
		byte[] dest = new byte[length];
		System.arraycopy(src, Segment.RUDP_HEADER_LEN, dest, 0, length);
		return dest;
	}

	public int read(byte[] dest) throws IOException {
		if (dest == null)
			throw new NullPointerException("dest is null");
		AppData data = socket.getReceiverBuffer().poll();
		byte[] src = data.getData();
		int length = src.length - Segment.RUDP_HEADER_LEN;
		System.arraycopy(src, Segment.RUDP_HEADER_LEN, dest, 0, length);
		return length;
	}

	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
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
			result = prime * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
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
