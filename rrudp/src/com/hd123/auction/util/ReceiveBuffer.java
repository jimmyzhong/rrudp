package com.hd123.auction.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.hd123.auction.UDTInputStream.AppData;

public class ReceiveBuffer {

	private final AppData[] buffer;

	private volatile int readPosition = 0;

	private int startSequenceNumber;
	private final AtomicInteger numValidChunks = new AtomicInteger(0);

	private final Condition emptyCondition;
	private final Condition fullCondition;
	private final ReentrantLock lock;

	// the size of the buffer
	private final int size;
	private final int sequenceSize;

	public ReceiveBuffer(int size, int sequenceSize, int initialSequenceNumber) {
		this.size = size;
		this.sequenceSize = sequenceSize;
		this.startSequenceNumber = initialSequenceNumber;
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
				return true;
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
			while (numValidChunks.get() == 0) {
				emptyCondition.await();
			}
			AppData r = buffer[readPosition];
			while (r == null) {
				emptyCondition.await();
			}
			increment();
			startSequenceNumber = (startSequenceNumber + 1) % sequenceSize;
			fullCondition.signal();
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return null;
	}

	public int getSize() {
		return size;
	}

	void increment() {
		buffer[readPosition] = null;
		readPosition++;
		if (readPosition == size)
			readPosition = 0;
		numValidChunks.decrementAndGet();
	}

	public AppData[] getBuffer() {
		return buffer;
	}

	public boolean isEmpty() {
		return numValidChunks.get() == 0;
	}

}
