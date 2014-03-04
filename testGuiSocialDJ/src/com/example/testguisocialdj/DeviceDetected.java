package com.example.testguisocialdj;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class DeviceDetected implements Runnable {
	private InetAddress pingAddr;
	NetworkInterface iFace;
	boolean active = false;
	
	public DeviceDetected(InetAddress pingAddr, NetworkInterface iFace) {
		this.pingAddr = pingAddr;
		this.iFace = iFace;
	}

	@Override
	public void run() {
		try {
			System.out.println("here");
			if (pingAddr.isReachable(iFace, 0, 50)) {
				active = true;
			}
			System.out.println("active: " + active);
		} catch (IOException e) {e.printStackTrace();}
	}
	
	public boolean checkDeviceStatus() {
		return active;
	}
	
	public InetAddress getDevice() {
		return pingAddr;
	}
}
