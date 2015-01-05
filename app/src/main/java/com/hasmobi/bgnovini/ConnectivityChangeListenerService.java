package com.hasmobi.bgnovini;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import com.hasmobi.bgnovini.util.App;

public class ConnectivityChangeListenerService extends Service {

	final BroadcastReceiver brConnectivityChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (App.isNetworkAvailable(context) && App.isNewsNeedsUpdating(context)) {
				// Last update older than 6 hours, update now
				context.startService(new Intent(context, NewsUpdaterService.class));
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
		if (receiverRegistered) {
			unregisterReceiver(brConnectivityChanged);
		}

		registerReceiver(brConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		receiverRegistered = true;

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (receiverRegistered) {
			unregisterReceiver(brConnectivityChanged);
			receiverRegistered = false;
		}
		super.onDestroy();
	}


}
