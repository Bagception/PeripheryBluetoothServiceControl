package de.uniulm.bagception.peripherybluetoothservicecontrol;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.logging.LOG;
import de.philipphock.android.lib.services.ServiceUtil;
import de.philipphock.android.lib.services.observation.ConstantFactory;
import de.philipphock.android.lib.services.observation.ServiceObservationActor;
import de.philipphock.android.lib.services.observation.ServiceObservationReactor;
import de.uniulm.bagception.bluetoothclientmessengercommunication.actor.BundleMessageActor;
import de.uniulm.bagception.bluetoothclientmessengercommunication.actor.BundleMessageReactor;
import de.uniulm.bagception.bluetoothclientmessengercommunication.service.BundleMessageHelper;
import de.uniulm.bagception.bundlemessageprotocol.BundleMessage;
import de.uniulm.bagception.bundlemessageprotocol.BundleMessage.BUNDLE_MESSAGE;
import de.uniulm.bagception.bundlemessageprotocol.entities.Item;
import de.uniulm.bagception.protocol.bundle.constants.Command;
import de.uniulm.bagception.protocol.bundle.constants.StatusCode;
import de.uniulm.bagception.services.ServiceNames;

public class BTControlActivity extends Activity implements
		ServiceObservationReactor, BundleMessageReactor {

	/*with this, we can observe if the Service we're connecting to, 
	 * is running (here the service is PeripheryBluetoothService AKA (client) BluetoothMiddleware)
	to use this, we need to
	 * initialize it (onCreate)
	 * register it (onResume)
	 * unregister it (onPause)
	
	This class needs the CallbackInterface ServiceObservationReactor (this)
	*/
	private ServiceObservationActor soActor;
	
	
	
	
	//after we start the service, the service should answer, if he doesn't, we check that with this handler. No Response=not installed 
	private final Handler checkinstalledHandler = new Handler();

	/*HelperClass to connect with the service and communicate via Messages
	to use this, we need to:
	 * initialize it (onCreate)
	 * register it (onServiceStarted)
	 * unregister it (onPause and when we force stop the service: startStopService)
		
		This class needs the CallbackInterface MessengerHelperCallback (this)
		
		We communicate via Messages
		There are 4 types of messages: 
		 * StatusMessages = StatusInformation of the BLuetoothMiddleware 
		 * CommandMessages = Commands to be send to the BluetoothMiddleware
		 * ResponseMessages = if the BluetoothMiddleware is not sure what to do, it sends a Response, then any system connected via MessengerHelper can answer via ResponseAnswer 
		 * BundleMessages = Messages between 2 BluetoothDevices. The BluetoothMiddleware only forwards those messages!!!
		 
		 
		 To extend those Messages, edit BluetoothServerMessengerCommunication .constants
	*/
	
	private BundleMessageActor bmActor; 
	private BundleMessageHelper messengerHelper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		soActor = new ServiceObservationActor(this,
				ServiceNames.BLUETOOTH_CLIENT_SERVICE); //init
		setContentView(R.layout.activity_btcontrol);
		bmActor = new BundleMessageActor(this);
		messengerHelper = new BundleMessageHelper(this);
	}

	//autogenerated  code
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.btcontrol, menu);
	// return true;
	// }
	//

	
	// button callbacks
	/**
	 * called when a user presses the start server/stop server button
	 * @param v
	 */
	public void startStopService(View v) {
		Button btv = (Button) v;
		Intent startStopService = new Intent(
				ServiceNames.BLUETOOTH_CLIENT_SERVICE);
		if (btv.getText().equals("stop Service")) {

			stopService(startStopService);
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

	

	
	/**
	 * called when a user presses send
	 * @param v
	 */
	public void onSendBtnClicked(View v) {
		EditText txt = (EditText) findViewById(R.id.message);
		String toSend = txt.getText().toString();

		final Bundle b = new Bundle();
		b.putString("cmd", "msg");
		b.putString("payload", toSend);

		//this is how we communicate with the service
		messengerHelper.sendMessageSendBundle(b);
	}

	public void onScanTriggerClicked(View v) {
		Bundle b = Command.getCommandBundle(Command.TRIGGER_SCAN_DEVICES);
		LOG.out(this, "SCAN BTN CLICKED");
		
		//this is how we communicate with the service if we want to send it after we hopefully stated the service
		considerDelayedSending(b);
		

	}

	
	private void considerDelayedSending(final Bundle b){
		
		if (!ServiceUtil.isServiceRunning(this, ServiceNames.BLUETOOTH_CLIENT_SERVICE)){
			startService(new Intent(ServiceNames.BLUETOOTH_CLIENT_SERVICE));
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
			
					messengerHelper.sendCommandBundle(b);
					
				}
			}, 300);
		}else{
			
			messengerHelper.sendCommandBundle(b);
		}
	}
	//A ping is used to check if the connection between BluetoothMiddleware and Activity works 
	//A PING is sent, then a PONG is recv.
	public void onPingClicked(View v) {

		Bundle b = Command.getCommandBundle(Command.PING);
		messengerHelper.sendCommandBundle(b);

	}

	// lifecycle methods

	@Override
	protected void onResume() {
		super.onResume();
		bmActor.register(this);
		onServiceStopped(null); //here we pretend that the service has stopped. If it has getForceResendStatusString will have to effect and the service is stopped, if it has getForceResendStatusString will trigger onServiceStarted 
		soActor.register(this);
		Intent broadcastRequest = new Intent();
		messengerHelper.sendCommandBundle(Command.getCommandBundle(Command.RESEND_STATUS));
		//broadcast answer is handled by ServiceObservationReactor
		//with this, we force the BluetoothMiddleware to resent if it is alive
		broadcastRequest
				.setAction(ConstantFactory 
						.getForceResendStatusString(ServiceNames.BLUETOOTH_CLIENT_SERVICE));  
		sendBroadcast(broadcastRequest);
	}

	@Override
	protected void onPause() {
		super.onPause();
		soActor.unregister(this);

		bmActor.unregister(this);

	}

	/**
	 * callback of ServiceObservationReactor
	 */
	@Override
	public void onServiceStarted(String serviceName) {
		Button startStopbutton = (Button) findViewById(R.id.startStopService);
		TextView status = (TextView) findViewById(R.id.ServiceStatus);
		startStopbutton.setText("stop Service");
		status.setText("started");
		status.setTextColor(Color.GREEN);

		
	}

	/**
	 * callback of ServiceObservationReactor
	 */
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
			
			v.setText("connected");
			v.setTextColor(Color.GREEN);
			
			break;

		case DISCONNECTED:
			
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
	public void onResponseAnswerMessage(Bundle b) {
		//nothing todo here
	}

	@Override
	public void onBundleMessageRecv(Bundle b) {
//		LOG.out(this, b);
//		BUNDLE_MESSAGE msg = BundleMessage.getInstance().getBundleMessageType(b);
//		switch (msg){
//		case NOT_A_BUNDLE_MESSAGE:
//			Toast.makeText(BTControlActivity.this, "unknown action", Toast.LENGTH_SHORT)
//			.show();
//				for (String key : b.keySet()) {
//					LOG.out(key, b.get(key));
//				}
//				
//				Toast.makeText(BTControlActivity.this, b.toString(), Toast.LENGTH_SHORT)
//						.show();
//			break;
//			
//		case ITEM_FOUND:
//		case ITEM_NOT_FOUND:
//			
//			Item i;
//			try {
//				i = BundleMessage.getInstance().toItemFound(b);
//				String itemMsgString = "item found: "+i.getName();
//				if (msg == BUNDLE_MESSAGE.ITEM_NOT_FOUND){
//					itemMsgString = "unknown tag: "+i.getIds().get(0);
//				}
//				Toast.makeText(BTControlActivity.this,itemMsgString , Toast.LENGTH_SHORT)
//				.show();
//			} catch (JSONException e) {
//				Toast.makeText(BTControlActivity.this, "error reading item", Toast.LENGTH_SHORT)
//				.show();
//			}
//			
//			break;
//		
//			
//		
//		}
//		//those messages come from the remote bluetooth device, not from the BluetoothMiddleware
		
	}
	@Override
	public void onBundleMessageSend(Bundle b) {
		// nothing todo here, this is part of the middleware
		
	}


}
