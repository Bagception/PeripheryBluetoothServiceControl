package de.uniulm.bagception.peripherybluetoothservicecontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.logging.LOG;
import de.philipphock.android.lib.services.messenger.MessengerService;
import de.philipphock.android.lib.services.observation.ConstantFactory;
import de.philipphock.android.lib.services.observation.ServiceObservationActor;
import de.philipphock.android.lib.services.observation.ServiceObservationReactor;
import de.uniulm.bagception.bluetoothservermessengercommunication.MessengerConstants;
import de.uniulm.bagception.services.ServiceNames;

public class BTControlActivity extends Activity implements
		ServiceObservationReactor {

	private ServiceObservationActor soActor;
	private final Handler checkinstalledHandler = new Handler();

	// communication with service
	private Messenger serviceMessenger;
	private boolean isConnectedWithService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		soActor = new ServiceObservationActor(this,
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
		setContentView(R.layout.activity_btcontrol);
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
			btv.setText("start Service");
			if(isConnectedWithService){
				unbindService(sconn);
			}
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
		Message m = Message.obtain(null,
				MessengerConstants.MESSAGE_BUNDLE_MESSAGE);
		m.setData(b);
		if (serviceMessenger==null){
			onSendButNotConnectedWithService();
			return;
		}
		try {
			serviceMessenger.send(m);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	private  void onSendButNotConnectedWithService(){
		Toast.makeText(this, "tried to send text but not connected with service", Toast.LENGTH_SHORT).show();
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
		
		if(isConnectedWithService){
			unbindService(sconn);
			isConnectedWithService=false;
		}
		
	}

	@Override
	public void onServiceStarted(String serviceName) {
		Button startStopbutton = (Button) findViewById(R.id.startStopService);
		TextView status = (TextView) findViewById(R.id.ServiceStatus);
		startStopbutton.setText("stop Service");
		status.setText("started");
		status.setTextColor(Color.GREEN);
		
		Intent runningService = new Intent(
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
		bindService(runningService, sconn, 0);
	}

	@Override
	public void onServiceStopped(String serviceName) {
		Button startStopbutton = (Button) findViewById(R.id.startStopService);
		startStopbutton.setText("start Service");
		TextView status = (TextView) findViewById(R.id.ServiceStatus);
		status.setText("stopped");
		status.setTextColor(Color.RED);
	}

	// IPC using messenger

	ServiceConnection sconn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
			isConnectedWithService = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
			isConnectedWithService = true;

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
						MessengerService.MSG_REGISTER_CLIENT);
				msg.replyTo = incomingMessenger;
				serviceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	// ###### IPC ######\\
	private final Handler incomingHandler = new Handler(new Handler.Callback() {

		// Handles incoming messages
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MessengerConstants.MESSAGE_BUNDLE_MESSAGE:
				Log.d(getClass().getName(), "handle "
						+ msg.getData().toString());
				for (String key : msg.getData().keySet()) {
					LOG.out(key, msg.getData().get(key));
				}
				Toast.makeText(BTControlActivity.this,
						msg.getData().toString(), Toast.LENGTH_SHORT).show();

				break;
			}
			return false;
		}
	});

	// delivered to the server, handles incoming messages
	private final Messenger incomingMessenger = new Messenger(incomingHandler);

	public void doUnbindService() {
		if (isConnectedWithService) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (serviceMessenger != null) {
				try {
					Message msg = Message.obtain(null,
							MessengerService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = incomingMessenger;
					serviceMessenger.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			unbindService(sconn);
			isConnectedWithService = false;

		}
	}

}
