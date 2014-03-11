package com.example.testguisocialdj;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectActivity extends Activity {

	//standard port for socket
	private static int standardPort = 8888;
	//standard port for not active ip address on network
	private static String nonActiveIp = "0.0.0.0";
	//Adapter
	private static MyAdapter myAdpt;
	
	//private RadioButton presentlyClicked = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_main);

		ArrayList<IP> activeServers = findHosts();

		ListView listView = (ListView)findViewById(R.id.listView);

		myAdpt = new MyAdapter(this, R.layout.connect_list, activeServers);

		listView.setAdapter(myAdpt);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.queue, menu);
		return true;
	}

	/**
	 * Finds all ip addresses with a socket open for this application to connect to.
	 * @return
	 */
	public ArrayList<IP> findHosts(){
		/*
		 * Gets the address of router connected to.
		 */
		final WifiManager manager = (WifiManager) super.getSystemService(WIFI_SERVICE);
		final DhcpInfo dhcp = manager.getDhcpInfo();
		final String address = Formatter.formatIpAddress(dhcp.gateway);

		//list of active servers on network
		ArrayList<IP> activeServers = new ArrayList<IP>();

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
				//checking if thread is done running, if it is create an IP for it
				if (!f.get().equals(nonActiveIp)) {
					IP ip = new IP(f.get(), false);
					activeServers.add(ip);
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
					return nonActiveIp; //default for not found
				}
			}
		});
	}

	/**
	 * This class is an adapter used to put radiobuttons/textview on each item in a listview.  
	 * @author Nathan
	 *
	 */
	private class MyAdapter extends ArrayAdapter<IP> {
		ArrayList<IP> activeServers;
		private RadioButton currentlyClicked;
		private boolean check = false;
		private Socket socket = null;

		public MyAdapter(Context context, int textViewResourceId, 
				ArrayList<IP> activeServers) {
			super(context, textViewResourceId, activeServers);
			this.activeServers = new ArrayList<IP>();
			this.activeServers.addAll(activeServers);
		}

		private class ViewHolder {
			TextView textViewIP;
			RadioButton radio;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));
			
			SharedPreferences settings = getSharedPreferences("connected", MODE_PRIVATE);
			
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.connect_list, null);

				holder = new ViewHolder();
				holder.textViewIP = (TextView) convertView.findViewById(R.id.textViewIP);
				holder.radio = (RadioButton) convertView.findViewById(R.id.radio);
				convertView.setTag(holder);	

				//temp variable to set to final to use when radio button is clicked on
				final TextView temp = holder.textViewIP;

				if (position == getCount() - 1 && check == false) {
					holder.radio.setChecked(true);
					currentlyClicked = holder.radio;
				} else {
					holder.radio.setChecked(false);
				}

				//on click for radio buttons
				holder.radio.setOnClickListener( new View.OnClickListener() {  
					public void onClick(View v) {  

						if (currentlyClicked != null) {
							if (currentlyClicked == null)
								currentlyClicked = (RadioButton) v;
							currentlyClicked.setChecked(true);
						}

						//tries to connect to server associated with radio button
						try {
							if(socket != null) 
								socket.close();
														
							socket = new Socket();
							//longer timeout because it takes an extra couple ms to close socket and try to reconnect
							socket.connect(new InetSocketAddress(temp.getText().toString().trim(), standardPort), 2000);
							Toast.makeText(getApplicationContext(), "Connected to Server: " + temp.getText().toString().trim(), Toast.LENGTH_SHORT).show();
							
							//save state of which button was selected in internal storage to use when acitivty is called again
							SharedPreferences settings = getSharedPreferences("connected", MODE_PRIVATE);
							SharedPreferences.Editor editor = settings.edit();
							editor.putString("currentlyConnected", temp.getText().toString().trim());
							editor.commit();
						} catch (Exception ex) {
							//server can't be connected to
							Toast.makeText(getApplicationContext(), "Error Connecting to Server: " + temp.getText().toString().trim(), Toast.LENGTH_SHORT).show();
						}
						
						if (currentlyClicked == v)
							return;

						currentlyClicked.setChecked(false);
						((RadioButton) v).setChecked(true);
						currentlyClicked = (RadioButton) v;

					}  
				});  
			} 
			else {
				holder = (ViewHolder) convertView.getTag();
			}

			//textview next to radio button
			IP ip = activeServers.get(position);
			holder.textViewIP.setText("   " +  ip.getAddress());
			holder.radio.setChecked(ip.isSelected());
			
			//checks the current server connected to when reopening the connect screen
			if(!(settings.getString("currentlyConnected", nonActiveIp).equals(nonActiveIp))){
				if(holder.textViewIP.getText().toString().trim().equals(settings.getString("currentlyConnected", nonActiveIp)))
						holder.radio.setChecked(true);
			}

			return convertView;

		}

	}
}