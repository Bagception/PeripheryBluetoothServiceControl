package de.uniulm.bagception.peripherybluetoothservicecontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import de.uniulm.bagception.protocol.bundle.constants.Command;
import de.uniulm.bagception.protocol.bundle.constants.Response;
import de.uniulm.bagception.protocol.bundle.constants.ResponseAnswer;
import de.uniulm.bagception.protocol.bundle.constants.StatusCode;
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
		messengerHelper = new MessengerHelper(this,
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.btcontrol, menu);
	// return true;
	// }
	//

	// button callbacks

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

	public void onScanTriggerClicked(View v) {
		Bundle b = Command.getCommandBundle(Command.TRIGGER_SCAN_DEVICES);
		LOG.out(this, "SCAN BTN CLICKED");
		messengerHelper.sendCommandBundle(b);

	}

	public void onPingClicked(View v) {

		Bundle b = Command.getCommandBundle(Command.PING);
		messengerHelper.sendCommandBundle(b);

	}

	// lifecycle methods

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

	// MessengerHelperCallback

	@Override
	public void onBundleMessage(Bundle b) {
		for (String key : b.keySet()) {
			LOG.out(key, b.get(key));
		}
		Toast.makeText(BTControlActivity.this, b.toString(), Toast.LENGTH_SHORT)
				.show();

	}

	@Override
	public void onError(Exception e) {
		e.printStackTrace();

	}

	@Override
	public void onResponseMessage(Bundle b) {
//		Response r = Response.getResponse(b);
//		switch (r) {
//		case Confirm_Established_Connection:
//			Toast.makeText(this, "establish?", Toast.LENGTH_SHORT).show();
//			Bundle answer = ResponseAnswer.getResponseAnswerBundle(ResponseAnswer.Confirm_Established_Connection);
//			answer.putBoolean(ResponseAnswer.EXTRA_KEYS.PAYLOAD, true);
//			messengerHelper.sendResponseBundle(answer);
//			
//			break;
//
//		default:
//			break;
//		}
	}

	@Override
	public void onStatusMessage(Bundle b) {
		StatusCode c = StatusCode.getStatusCode(b);
		TextView v = (TextView) findViewById(R.id.ConnectionInfo);
		switch (c) {
		case CONNECTED:
			Toast.makeText(BTControlActivity.this, "Connected to server",
					Toast.LENGTH_SHORT).show();
			
			v.setText("connected");
			v.setTextColor(Color.GREEN);
			
			break;

		case DISCONNECTED:
			Toast.makeText(BTControlActivity.this, "Disconnected from server",
					Toast.LENGTH_SHORT).show();
		
			v.setText("disconnected");
			v.setTextColor(Color.RED);
			break;

		case ERROR:
			String error = b.getString(StatusCode.EXTRA_KEYS.ERROR_MESSAGE);
			Toast.makeText(BTControlActivity.this, "Error: "+error,
					Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}

	}

	@Override
	public void onCommandMessage(Bundle b) {
		Command c = Command.getCommand(b);
		switch (c) {
		case PONG:
			Toast.makeText(BTControlActivity.this, "PONG recv",
					Toast.LENGTH_SHORT).show();
		default:
			LOG.out(this, c.getCommandCode());

		}
	}

	@Override
	public void connectedWithRemoteService() {
		messengerHelper.sendCommandBundle(Command.getCommandBundle(Command.RESEND_STATUS));
	}

	@Override
	public void disconnectedFromRemoteService() {
		// TODO Auto-generated method stub
		
	}

}
