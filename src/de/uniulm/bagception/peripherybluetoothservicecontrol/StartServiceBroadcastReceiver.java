package de.uniulm.bagception.peripherybluetoothservicecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.philipphock.android.lib.services.ServiceUtil;
import de.uniulm.bagception.services.ServiceNames;

public class StartServiceBroadcastReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context arg0, Intent arg1) {

		if (!ServiceUtil.isServiceRunning(arg0, ServiceNames.BLUETOOTH_CLIENT_SERVICE)){
			Intent startServiceIntent = new Intent(ServiceNames.BLUETOOTH_CLIENT_SERVICE);
			arg0.startService(startServiceIntent);
			
		}
		
	}

}
