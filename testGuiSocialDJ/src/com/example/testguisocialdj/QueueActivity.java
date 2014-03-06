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
import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class QueueActivity extends Activity {

	//standard port for socket
	private static int standardPort = 8888;
	//standard port for not active ip address on network
	private static String nonActiveIp = "0.0.0.0";
	//Adapter
	private static MyAdapter myAdpt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_main);
		
		ArrayList<IP> activeServers = findHosts();
			   
		ListView listView = (ListView)findViewById(R.id.listView1);
		
		myAdpt = new MyAdapter(this, R.layout.activity_queue, activeServers);
		
		listView.setAdapter(myAdpt);
		
		/*listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				IP ip = (IP) parent.getItemAtPosition(position);
				Toast.makeText(getApplicationContext(),
						"Clicked on Row: " + ip.getAddress(), 
						Toast.LENGTH_LONG).show();
			}
		});*/
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.queue, menu);
		return true;
	}
	
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
					return "0.0.0.0"; //default for not found
				}
			}
		});
	}

	private class MyAdapter extends ArrayAdapter<IP> {
		ArrayList<IP> activeServers;
		private RadioButton currentlyClicked;
		private boolean check = false;

		public MyAdapter(Context context, int textViewResourceId, 
				ArrayList<IP> activeServers) {
			super(context, textViewResourceId, activeServers);
			this.activeServers = new ArrayList<IP>();
			this.activeServers.addAll(activeServers);
		}

		private class ViewHolder {
			TextView code;
			RadioButton name;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			 ViewHolder holder = null;
			   Log.v("ConvertView", String.valueOf(position));
			 
			   if (convertView == null) {
			   LayoutInflater vi = (LayoutInflater)getSystemService(
			     Context.LAYOUT_INFLATER_SERVICE);
			   convertView = vi.inflate(R.layout.activity_queue, null);
			 
			   holder = new ViewHolder();
			   holder.code = (TextView) convertView.findViewById(R.id.code);
			   holder.name = (RadioButton) convertView.findViewById(R.id.radio);
			   convertView.setTag(holder);
			   
			   //temp variable to set to final to use when radio button is clicked on
			   final TextView temp = holder.code;
			   
			   if (position == getCount() - 1 && check == false) {
		            holder.name.setChecked(true);
		            currentlyClicked = holder.name;
		        } else {
		            holder.name.setChecked(false);
		        }
			 
			    holder.name.setOnClickListener( new View.OnClickListener() {  
			     public void onClick(View v) {  
			    	 if (currentlyClicked != null) {
		                    if (currentlyClicked == null)
		                    	currentlyClicked = (RadioButton) v;
		                    currentlyClicked.setChecked(true);
		                }

		                if (currentlyClicked == v)
		                    return;
		                
		                System.out.println(currentlyClicked.getId());
		                System.out.println(temp.getText());
		                
		                currentlyClicked.setChecked(false);
		                ((RadioButton) v).setChecked(true);
		                currentlyClicked = (RadioButton) v;

		                try {
		                  Socket socket = new Socket();
						  socket.connect(new InetSocketAddress(temp.getText().toString().trim(), standardPort), 200);
						  Toast.makeText(getApplicationContext(), "Connected to Server: " + temp.getText().toString().trim(), Toast.LENGTH_SHORT).show();
		                } catch (Exception ex) {
                             //server can't be connected to
		                	Toast.makeText(getApplicationContext(), "Error Connecting to Server: " + temp.getText().toString().trim(), Toast.LENGTH_SHORT).show();
						}
			     }  
			    });  
			   } 
			   else {
			    holder = (ViewHolder) convertView.getTag();
			   }
			 
			   IP ip = activeServers.get(position);
			   holder.code.setText("   " +  ip.getAddress());
			   holder.name.setChecked(ip.isSelected());
			 
			   return convertView;
			 
		}

	}
}