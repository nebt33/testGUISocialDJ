package com.example.testguisocialdj;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class QueueActivity extends Activity {

	//standard port for socket
	private static int standardPort = 8888;
	//standard port for not active ip address on network
	private static String nonActiveIp = "0.0.0.0";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_queue);
		
		List<Map<String,String>> activeServers = findHosts();
			   
		ListView list = (ListView)findViewById(R.id.listView);
		
		SimpleAdapter simpleAdpt = new SimpleAdapter(this, activeServers, android.R.layout.simple_list_item_1, new String[] {"host"}, new int[] {android.R.id.text1});
		
		list.setAdapter(simpleAdpt);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.queue, menu);
		return true;
	}
	
	public List<Map<String,String>> findHosts(){
		/*
		 * Gets the address of router connected to.
		 */
		final WifiManager manager = (WifiManager) super.getSystemService(WIFI_SERVICE);
		final DhcpInfo dhcp = manager.getDhcpInfo();
		final String address = Formatter.formatIpAddress(dhcp.gateway);

		//list of active servers on network
		List<Map<String,String>> activeServers = new ArrayList<Map<String,String>>();
		
		try {
			NetworkInterface iFace = NetworkInterface
					.getByInetAddress(InetAddress.getByName(address));
		} catch(IOException e) {}
			
		//thread pool to check ip's concurrently
		final ExecutorService es = Executors.newFixedThreadPool(50);
		  final int timeout = 200;
		  final List<Future<String>> futures = new ArrayList<Future<String>>();
		  for (int subaddress = 0; subaddress <= 255; subaddress++) {
			  // build the next IP address
			  String addr = address;
			  addr = addr.substring(0, addr.lastIndexOf('.') + 1) + subaddress;
				  
			  futures.add(portIsOpen(es, addr, standardPort, timeout));
		  }
		  es.shutdown();
		  for (final Future<String> f : futures) {
		    try {
				if (!f.get().equals(nonActiveIp)) {
					activeServers.add(createHost("host",f.get()));
				}
			} catch (InterruptedException e) {e.printStackTrace();
			} catch (ExecutionException e) {}
		  }
		
		return activeServers;

	}
	
	/**
	 * This method will check to see if a socket is open on this port on a specfic computer(i.e. looks for a server that would be usable for the application)
	 * @param es
	 * @param ip
	 * @param port
	 * @param timeout
	 * @return
	 */
	public static Future<String> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
		return es.submit(new Callable<String>() {
			@Override public String call() {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					socket.close();
					return ip;
				} catch (Exception ex) {
					return "0.0.0.0"; //default for not found
				}
			}
		});
	}
	
	private HashMap<String, String> createHost(String key, String id) {
		HashMap<String, String> host = new HashMap<String,String>();
		host.put(key, id);
		return host;
	}
}
