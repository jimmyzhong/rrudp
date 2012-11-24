package com.hd123.auction.receiver;

import com.hd123.auction.util.Util;

public class ReceiverLossListEntry implements Comparable<ReceiverLossListEntry> {

	private final long sequenceNumber;
	private	long lastFeedbacktime;
	private long k = 2;

	public ReceiverLossListEntry(long sequenceNumber){
		if(sequenceNumber<=0){
			throw new IllegalArgumentException("Got sequence number "+sequenceNumber);
		}
		this.sequenceNumber = sequenceNumber;	
		this.lastFeedbacktime=Util.getCurrentTime();
	}


	/**
	 * call once when this seqNo is fed back in NAK
	 */
	public void feedback(){
		k++;
		lastFeedbacktime=Util.getCurrentTime();
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long getK() {
		return k;
	}

	public long getLastFeedbackTime() {
		return lastFeedbacktime;
	}
	
	public int compareTo(ReceiverLossListEntry o) {
		return (int)(sequenceNumber-o.sequenceNumber);
	}

	@Override
	public String toString(){
		return sequenceNumber+"[k="+k+",time="+lastFeedbacktime+"]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (k ^ (k >>> 32));
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
		ReceiverLossListEntry other = (ReceiverLossListEntry) obj;
		if (sequenceNumber != other.sequenceNumber)
			return false;
		return true;
	}

}
