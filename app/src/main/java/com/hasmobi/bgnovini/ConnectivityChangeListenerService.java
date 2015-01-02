package com.hasmobi.bgnovini;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import java.util.concurrent.TimeUnit;

public class ConnectivityChangeListenerService extends Service {

	final BroadcastReceiver brConnectivityChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(isNetworkAvailable(context)){
				long lastUpdateTimestamp = context.getSharedPreferences("updates", Context.MODE_PRIVATE).getLong("last_update", -1);

				long nextScheduledUpdate = lastUpdateTimestamp + TimeUnit.HOURS.toMillis(6);
				if (nextScheduledUpdate < System.currentTimeMillis()) {
					// Last update older than 6 hours, update now
					context.startService(new Intent(context, NewsUpdaterService.class));
				}
			}
		}
	};

	private boolean receiverRegistered = false;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(receiverRegistered){
			unregisterReceiver(brConnectivityChanged);
			receiverRegistered = false;
		}

		registerReceiver(brConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		receiverRegistered = true;

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if(receiverRegistered){
			unregisterReceiver(brConnectivityChanged);
			receiverRegistered = false;
		}
		super.onDestroy();
	}

	private boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		//should check null because in air plan mode it will be null
		return (netInfo != null && netInfo.isConnected());
	}
}
