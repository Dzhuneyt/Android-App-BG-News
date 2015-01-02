package com.hasmobi.bgnovini;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.TimeUnit;

public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent(context, NewsUpdaterService.class);
			PendingIntent pintent = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarm.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), TimeUnit.HOURS.toMillis(3), pintent);

			// This will listen for internet enabled/disabled constantly
			// and if internet enabled and last update is old will kick start
			// the refresher service
			context.startService(new Intent(context, ConnectivityChangeListenerService.class));
		}
	}
}
