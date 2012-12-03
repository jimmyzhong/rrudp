package com.hd123.auction.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.hd123.auction.UDPInputStream.AppData;

public class ReceiveBuffer {

	private final AppData[] buffer;

	private volatile int readPosition = -1;

	private int startSequenceNumber;
	private final AtomicInteger numValidChunks = new AtomicInteger(0);

	private final Condition emptyCondition;
	private final Condition fullCondition;
	private final ReentrantLock lock;

	private final int size;
	private final int sequenceSize;

	/**
	 * @param size  缓冲区大小
	 * @param sequenceSize 序列号大小
	 */
	public ReceiveBuffer(int size, int sequenceSize) {
		this.size = size;
		this.sequenceSize = sequenceSize;
		this.buffer = new AppData[size];

		lock = new ReentrantLock(false);
		emptyCondition = lock.newCondition();
		fullCondition = lock.newCondition();
	}

	public boolean offer(AppData data) {
		lock.lock();
		try {
			while (numValidChunks.get() == size) {
				fullCondition.await();
			}
			int seq = data.getSequenceNumber();
			int index = (seq - startSequenceNumber + readPosition) % size;
			if (buffer[index] != null)
				return false;
			buffer[index] = data;
			numValidChunks.incrementAndGet();
			emptyCondition.signal();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return false;
	}

	public AppData poll() {
		lock.lock();
		try {
			if(readPosition == -1)
				throw new RuntimeException("readPosition  not set");
			while (numValidChunks.get() == 0) {
				emptyCondition.await();
			}
			AppData r = buffer[readPosition];
			while (r == null) {
				emptyCondition.await();
			}
			buffer[readPosition] = null;
			readPosition = (readPosition + 1) % size;
			numValidChunks.decrementAndGet();
			startSequenceNumber = (startSequenceNumber + 1) % sequenceSize;
			fullCondition.signal();
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		throw new RuntimeException("should not run here");
	}

	public int getSize() {
		return size;
	}

	public AppData[] getBuffer() {
		return buffer;
	}

	public boolean isEmpty() {
		return numValidChunks.get() == 0;
	}

	public void setInitialDataSequenceNumber(int i) {
		startSequenceNumber = i;
		readPosition = i;
	}

}
