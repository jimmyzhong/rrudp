package com.hd123.auction;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hd123.auction.util.ReceiveBuffer;

public class UDTInputStream extends InputStream {

	private final UDTSocket socket;

	private final ReceiveBuffer receiveBuffer;

	private final AtomicBoolean expectMoreData=new AtomicBoolean(true);

	private volatile boolean closed=false;

	private volatile boolean blocking=true;

	public UDTInputStream(UDTSocket socket) throws IOException{
		this.socket=socket;
		int capacity=socket!=null? 2 * socket.getSession().getFlowWindowSize() : 128 ;
		long initialSequenceNum=socket!=null?socket.getSession().getInitialSequenceNumber():1;
		receiveBuffer=new ReceiveBuffer(capacity,socket.getSession().getSequenceSize(),initialSequenceNum);
	}

	private final byte[] single=new byte[1];

	@Override
	public int read() throws IOException{
		int b=0;
		while(b==0)
			b=read(single);

		if(b>0){
			return single[0];
		}
		else {
			return b;
		}
	}

	private AppData currentChunk=null;
	//offset into currentChunk
	int offset=0;

	@Override
	public int read(byte[]target)throws IOException{
		try{
			int read=0;
			updateCurrentChunk(false);
			while(currentChunk!=null){
				byte[]data=currentChunk.data;
				int length=Math.min(target.length-read,data.length-offset);
				System.arraycopy(data, offset, target, read, length);
				read+=length;
				offset+=length;
				//check if chunk has been fully read
				if(offset>=data.length){
					currentChunk=null;
					offset=0;
				}

				//if no more space left in target, exit now
				if(read==target.length){
					return read;
				}

				updateCurrentChunk(blocking && read==0);
			}

			if(read>0)return read;
			if(closed)return -1;
			if(expectMoreData.get() || !receiveBuffer.isEmpty())return 0;
			//no more data
			return -1;

		}catch(Exception ex){
			IOException e= new IOException();
			e.initCause(ex);
			throw e;
		}
	}

	/**
	 * Reads the next valid chunk of application data from the queue<br/>
	 * 
	 * In blocking mode,this method will block until data is available or the socket is closed, 
	 * otherwise it will wait for at most 10 milliseconds.
	 * 
	 * @throws InterruptedException
	 */
	private void updateCurrentChunk(boolean block)throws IOException{
		if(currentChunk!=null)return;

		while(true){
			try{
				if(block){
					currentChunk=receiveBuffer.poll(1, TimeUnit.MILLISECONDS);
					while (!closed && currentChunk==null){
						currentChunk=receiveBuffer.poll(1000, TimeUnit.MILLISECONDS);
					}
				}
				else currentChunk=receiveBuffer.poll(10, TimeUnit.MILLISECONDS);
				
			}catch(InterruptedException ie){
				IOException ex=new IOException();
				ex.initCause(ie);
				throw ex;
			}
			return;
		}
	}

	/**
	 * new application data
	 * @param data
	 * 
	 */
	protected boolean haveNewData(long sequenceNumber,byte[]data)throws IOException{
		return receiveBuffer.offer(new AppData(sequenceNumber,data));
	}

	@Override
	public void close()throws IOException{
		if(closed)return;
		closed=true;
		noMoreData();
	}

	public UDTSocket getSocket(){
		return socket;
	}

	/**
	 * sets the blocking mode
	 * @param block
	 */
	public void setBlocking(boolean block){
		this.blocking=block;
	}

	public int getReceiveBufferSize(){
		return receiveBuffer.getSize();
	}
	
	/**
	 * notify the input stream that there is no more data
	 * @throws IOException
	 */
	protected void noMoreData()throws IOException{
		expectMoreData.set(false);
	}

	/**
	 * used for storing application data and the associated
	 * sequence number in the queue in ascending order
	 */
	public static class AppData implements Comparable<AppData>{
		final int sequenceNumber;
		final byte[] data;
		
		

		public AppData(int sequenceNumber, byte[] data){
			this.sequenceNumber=sequenceNumber;
			this.data=data;
		}

		public int compareTo(AppData o) {
			return (int)(sequenceNumber-o.sequenceNumber);
		}

		public String toString(){
			return sequenceNumber+"["+data.length+"]";
		}

		public int getSequenceNumber(){
			return sequenceNumber;
		}
		
		public byte[] getData() {
			return data;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			AppData other = (AppData) obj;
			if (sequenceNumber != other.sequenceNumber)
				return false;
			return true;
		}


	}

}
