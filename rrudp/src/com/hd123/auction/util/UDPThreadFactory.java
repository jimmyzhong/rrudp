package com.hd123.auction.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class UDPThreadFactory implements ThreadFactory {

	private static final AtomicInteger num=new AtomicInteger(0);
	
	private static UDPThreadFactory theInstance=null;
	
	public static synchronized UDPThreadFactory get(){
		if(theInstance==null)
			theInstance=new UDPThreadFactory();
		return theInstance;
	}
	
	public Thread newThread(Runnable r) {
		Thread t=new Thread(r);
		t.setName("UDT-Thread-"+num.incrementAndGet());
		return t;
	}

}
