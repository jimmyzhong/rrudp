package com.hd123.auction.receiver;
import java.util.concurrent.PriorityBlockingQueue;


public class ReceiverLossList {

	private final PriorityBlockingQueue<ReceiverLossListEntry> backingList;
	
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
	
	public ReceiverLossListEntry getFirstEntry(){
		return backingList.peek();
	}
	
	public int size(){
		return backingList.size();
	}
	
	public String toString(){
		return backingList.toString();
	}
	
	
}
