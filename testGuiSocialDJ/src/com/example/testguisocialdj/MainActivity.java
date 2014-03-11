package com.example.testguisocialdj;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	//standard port for socket
	private static int standardPort = 8888;
	//standard port for not active ip address on network
	private static String nonActiveIp = "0.0.0.0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//if the application was closed a server is still up
		//reconnect to server when application is restarted
		Socket socket = null;
		SharedPreferences settings = getSharedPreferences("connected", MODE_PRIVATE);
		
		if(!(settings.getString("currentlyConnected", nonActiveIp).equals(nonActiveIp))){
			socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(settings.getString("currentlyConnected", nonActiveIp).trim(), standardPort), 2000);
			} catch (IOException e) {e.printStackTrace();}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/** Called when the user clicks the controls button*/
	public void createConnect(View view){
		//Does something in response to the button being clicked
		Intent intent = new Intent(this, ConnectActivity.class);
		startActivity(intent);
	}
	
	@Override
	public void finish() {
		//deleting stored memory
		SharedPreferences settings = getSharedPreferences("connected", MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("currentlyConnected");
		editor.commit();
	}

}
