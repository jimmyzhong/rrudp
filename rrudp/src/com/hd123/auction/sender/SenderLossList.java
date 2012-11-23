

package com.hd123.auction.sender;
import java.util.LinkedList;

/**
 * stores the sequence number of the lost packets in increasing order
 */
public class SenderLossList {

	private final LinkedList<Long> backingList;

	/**
	 * create a new sender lost list
	 */
	public SenderLossList(){
		backingList = new LinkedList<Long>();
	}

	public void insert(Long obj){
		synchronized (backingList) {
			if(!backingList.contains(obj)){
				for(int i=0;i<backingList.size();i++){
					if(obj<backingList.getFirst()){
						backingList.add(i,obj);	
						return;
					}
				}
				backingList.add(obj);
			}
		}
	}

	/**
	 * retrieves the loss list entry with the lowest sequence number
	 */
	public Long getFirstEntry(){
		synchronized(backingList){
			return backingList.poll();
		}
	}

	public boolean isEmpty(){
		return backingList.isEmpty();
	}

	public int size(){
		return backingList.size();
	}

	public String toString(){
		synchronized (backingList) {
			return backingList.toString();	
		}
	}
}
