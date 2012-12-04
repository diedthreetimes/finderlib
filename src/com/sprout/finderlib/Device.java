package com.sprout.finderlib;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;

public class Device {
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
