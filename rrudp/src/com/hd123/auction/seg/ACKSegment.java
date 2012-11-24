package com.hd123.auction.seg;

public class ACKSegment extends Segment
{
	public ACKSegment()
    {
    }

    public ACKSegment(int seqn, int ackn)
    {
        init(ACK_FLAG, seqn, RUDP_HEADER_LEN);
        setAck(ackn);
    }

    public String type()
    {
        return "ACK";
    }
}
