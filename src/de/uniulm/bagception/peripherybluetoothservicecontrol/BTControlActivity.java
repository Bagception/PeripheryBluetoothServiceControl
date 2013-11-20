package de.uniulm.bagception.peripherybluetoothservicecontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.logging.LOG;
import de.philipphock.android.lib.services.observation.ConstantFactory;
import de.philipphock.android.lib.services.observation.ServiceObservationActor;
import de.philipphock.android.lib.services.observation.ServiceObservationReactor;
import de.uniulm.bagception.bluetoothservermessengercommunication.messenger.MessengerHelper;
import de.uniulm.bagception.bluetoothservermessengercommunication.messenger.MessengerHelperCallback;
import de.uniulm.bagception.services.ServiceNames;

public class BTControlActivity extends Activity implements
		ServiceObservationReactor, MessengerHelperCallback {

	private ServiceObservationActor soActor;
	private final Handler checkinstalledHandler = new Handler();

	private MessengerHelper messengerHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		soActor = new ServiceObservationActor(this,
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
		setContentView(R.layout.activity_btcontrol);
		messengerHelper = new MessengerHelper(this, ServiceNames.BLUETOOTH_CLIENT_SERVICE);
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.btcontrol, menu);
	// return true;
	// }
	//

	//button callbacks
	
	public void startStopService(View v) {
		Button btv = (Button) v;
		Intent startStopService = new Intent(
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
		if (btv.getText().equals("stop Service")) {
			
			stopService(startStopService);
			messengerHelper.unregister(this);
			btv.setText("start Service");
			
		} else {
			startService(startStopService);
			btv.setText("stop Service");
			checkinstalledHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					TextView status = (TextView) findViewById(R.id.ServiceStatus);
					if (status.getText().equals("stopped")) {
						status.setText("not installed");
						status.setTextColor(Color.BLUE);
					}

				}
			}, 100);
		}

	}
	
	public void onSendBtnClicked(View v) {
		EditText txt = (EditText) findViewById(R.id.message);
		String toSend = txt.getText().toString();
	
		Bundle b = new Bundle();
		b.putString("cmd", "msg");
		b.putString("payload", toSend);
		
		messengerHelper.sendMessageBundle(b);

	}

	
	public void onPingClicked(View v) {
		Bundle b = new Bundle();
		b.putString("cmd", "PING");
		messengerHelper.sendMessageBundle(b);


	}
	
	
	//lifecycle methods
	
	@Override
	protected void onResume() {
		super.onResume();
		onServiceStopped(null);
		soActor.register(this);
		Intent broadcastRequest = new Intent();
		broadcastRequest
				.setAction(ConstantFactory
						.getForceResendStatusString(ServiceNames.BLUETOOTH_CLIENT_SERVICE));
		sendBroadcast(broadcastRequest);
	}

	@Override
	protected void onPause() {
		super.onPause();
		soActor.unregister(this);
		
		messengerHelper.unregister(this);
		
	}

	@Override
	public void onServiceStarted(String serviceName) {
		Button startStopbutton = (Button) findViewById(R.id.startStopService);
		TextView status = (TextView) findViewById(R.id.ServiceStatus);
		startStopbutton.setText("stop Service");
		status.setText("started");
		status.setTextColor(Color.GREEN);
		
		messengerHelper.register(this);
	}

	@Override
	public void onServiceStopped(String serviceName) {
		Button startStopbutton = (Button) findViewById(R.id.startStopService);
		startStopbutton.setText("start Service");
		TextView status = (TextView) findViewById(R.id.ServiceStatus);
		status.setText("stopped");
		status.setTextColor(Color.RED);
	}


	
	//MessengerHelperCallback
	
	@Override
	public void onBundleMessage(Bundle b) {
		Log.d(getClass().getName(), "handle "
				+ b.toString());
		for (String key : b.keySet()) {
			LOG.out(key, b.get(key));
		}
		Toast.makeText(BTControlActivity.this,
				b.toString(), Toast.LENGTH_SHORT).show();
				
	}

	@Override
	public void onError(Exception e) {
		e.printStackTrace();
		
	}

	@Override
	public void onResponseMessage(Bundle b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusMessage(Bundle b) {
		// TODO Auto-generated method stub
		
	}

}
