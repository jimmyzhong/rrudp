package com.hd123.auction;

import java.net.InetAddress;

public class Destination {

	private final int port;

	private final InetAddress address;
	
	public Destination(InetAddress address, int port){
		this.address=address;
		this.port=port;
	}
	
	public InetAddress getAddress(){
		return address;
	}
	
	public int getPort(){
		return port;
	}
	
	public String toString(){
		return "Destination ["+address.getHostName()+" port="+port +"]";
	} 

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
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
		Destination other = (Destination) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	
}
