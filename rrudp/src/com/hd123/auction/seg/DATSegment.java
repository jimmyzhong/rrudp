package com.hd123.auction.seg;

public class DATSegment extends Segment {
	private byte[] data;

	public DATSegment() {
	}

	public DATSegment(int seqn, int ackn, byte[] b) {
		this(seqn, ackn, b, 0, b.length);
	}

	public DATSegment(int seqn, int ackn, byte[] b, int off, int len) {
		init(ACK_FLAG, seqn, RUDP_HEADER_LEN);
		setAck(ackn);
		data = new byte[len];
		System.arraycopy(b, off, data, 0, len);
	}

	@Override
	public int length() {
		return data.length + super.length();
	}

	@Override
	public String type() {
		return "DAT";
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public byte[] getBytes() {
		byte[] buffer = super.getBytes();
		System.arraycopy(data, 0, buffer, RUDP_HEADER_LEN, data.length);
		return buffer;
	}

	@Override
	public void parseBytes(byte[] buffer, int off, int len) {
		super.parseBytes(buffer, off, len);
		data = new byte[len - RUDP_HEADER_LEN];
		System.arraycopy(buffer, off + RUDP_HEADER_LEN, data, 0, data.length);
	}

}
