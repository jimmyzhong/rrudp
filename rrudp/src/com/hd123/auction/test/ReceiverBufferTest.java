package com.hd123.auction.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.hd123.auction.UDPInputStream.AppData;
import com.hd123.auction.util.ReceiveBuffer;

public class ReceiverBufferTest {

	private ReceiveBuffer buf;
	AtomicInteger num = new AtomicInteger(-1);
	String s = "hh";

	private static List<Integer> nums = new ArrayList<Integer>();
	static{
		nums.add(1);
		nums.add(2);
		nums.add(3);
		nums.add(5);
		nums.add(6);
		nums.add(7);
		nums.add(9);
		nums.add(10);
		nums.add(10);
		nums.add(4);
		nums.add(8);
		nums.add(11);
		nums.add(12);
		nums.add(13);
	}
	public static void main(String[] args) {
		ReceiverBufferTest test = new ReceiverBufferTest();
		test.buf = new ReceiveBuffer(10, 80);
		OfferThread off = test.new OfferThread();
		PollThread pull = test.new PollThread();
		LookThread look = test.new LookThread();
		off.start();
		pull.start();
		look.start();

	}

	class OfferThread extends Thread {
		@Override
		public void run() {
			while (true) {
				num.incrementAndGet();
				AppData data = new AppData(nums.remove(0),
						(s + num.get()).getBytes());
				System.out.println("put id:" + data.getSequenceNumber()
						+ "  data:" + new String(data.getData()));
				buf.offer(data);
				sleept(1000);
			}
		}
	}

	class PollThread extends Thread {
		@Override
		public void run() {
			while (true) {
				AppData data = buf.poll();
				if (data != null) {
					System.out.println("take id:" + data.getSequenceNumber()
							+ "  data:" + new String(data.getData()));
				} else {
					System.out.println("data is null");
				}
				sleept(1000);
			}
		}
	}

	class LookThread extends Thread {
		@Override
		public void run() {
			while (true) {
				AppData[] appDate = buf.getBuffer();
				System.out.print("static:");
				for (int i = 0; i<appDate.length;i++) {
					AppData data = appDate[i];
					if(data != null){
					System.out.print("[ id:" + data.getSequenceNumber()
							+ "  data:" + new String(data.getData()) +"]");
					}
					else{
						System.out.print("  null:" + i);
					}
				}
				System.out.println();
				sleept(3000);
			}
		}
	}

	public static void sleept(int time) {
		try {
			int t = Math.abs(new Random().nextInt(time));
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
