package com.hd123.auction.receiver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import com.hd123.auction.util.Util;


/**
 * the receiver loss list stores information about lost packets,
 * ordered by increasing sequence number.
 * 
 * @see ReceiverLossListEntry
 */
public class ReceiverLossList {

	private final PriorityBlockingQueue<ReceiverLossListEntry>backingList;
	
	public ReceiverLossList(){
		backingList = new PriorityBlockingQueue<ReceiverLossListEntry>(32);
	}
	
	public void insert(ReceiverLossListEntry entry){
		synchronized (backingList) {
			if(!backingList.contains(entry)){
				backingList.add(entry);
			}
		}
	}

	public void remove(long seqNo){
		backingList.remove(new ReceiverLossListEntry(seqNo));
	}
	
	public boolean contains(ReceiverLossListEntry obj){
		return backingList.contains(obj);
	}
	
	public boolean isEmpty(){
		return backingList.isEmpty();
	}
	
	/**
	 * read (but NOT remove) the first entry in the loss list
	 * @return
	 */
	public ReceiverLossListEntry getFirstEntry(){
		return backingList.peek();
	}
	
	public int size(){
		return backingList.size();
	}
	
	/**
	 * return all sequence numbers whose last feedback time is larger than k*RTT
	 * 
	 * @param RTT - the current round trip time
	 * @param doFeedback - true if the k parameter should be increased and the time should 
	 * be reset (using {@link ReceiverLossListEntry#feedback()} )
	 * @return
	 */
	public List<Long>getFilteredSequenceNumbers(long RTT, boolean doFeedback){
		List<Long>result=new ArrayList<Long>();
		ReceiverLossListEntry[]sorted=backingList.toArray(new ReceiverLossListEntry[0]);
		Arrays.sort(sorted);
		for(ReceiverLossListEntry e: sorted){
			if( (Util.getCurrentTime()-e.getLastFeedbackTime())>e.getK()*RTT){
				result.add(e.getSequenceNumber());
				if(doFeedback)e.feedback();
			}
		}
		return result;
	}
	
	public String toString(){
		return backingList.toString();
	}
	
	
}
