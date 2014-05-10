package com.sprout.finderlib.communication;

import java.io.Serializable;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;

public class Device implements Serializable{
  private static final long serialVersionUID = -4538812945263505278L;
  
  
  private String name;
	private String address;
	
	Device(String name, String address){
		this.name = name;
		this.address = address;
	}
	
	Device(BluetoothDevice bd){
		name = bd.getName();
		address = bd.getAddress();
	}
	
	Device(WifiP2pDevice wd){
		name = wd.deviceName;
		address = wd.deviceAddress;
	}
	
	@Override
	public int hashCode() {
	  return address.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
	  if (!(other instanceof Device)) {
	    return false;
	  }
	  
	  return address.equals(((Device) other).address);
	}
	
	public String getName(){
		return name;
	}
	public String getAddress(){
		return address;
	}
	
	public String toString(){
		return name;
	}
}
