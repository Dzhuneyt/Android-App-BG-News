package com.hasmobi.bgnovini.util;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		Parse.enableLocalDatastore(getApplicationContext());

		Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

		ParseObject.registerSubclass(FavoriteSource.class);
		ParseObject.registerSubclass(Source.class);
		ParseObject.registerSubclass(NewsArticle.class);

		Parse.initialize(getApplicationContext(), "u2GANc30nVAXDWrRN2wVb1AvnfhtGi99aX4ZKbYC", "HlOCcz5I5EFhb0pfILHGoHuT9IOj4m2kHRnLxe9s");

		ParseInstallation.getCurrentInstallation().saveInBackground();

		ParseUser.enableAutomaticUser();
		ParseUser.getCurrentUser().saveInBackground();

		ParseACL defaultACL = new ParseACL();
		ParseACL.setDefaultACL(defaultACL, true);

		Fresco.initialize(this);

	}

	public static boolean isNewsNeedsUpdating(Context context) {
		long lastUpdateTimestamp = context.getSharedPreferences("updates", MODE_PRIVATE).getLong("last_update", -1);

		long nextScheduledUpdate = lastUpdateTimestamp + TimeUnit.HOURS.toMillis(6);
		if (lastUpdateTimestamp > 0 && nextScheduledUpdate > System.currentTimeMillis()) {
			// Last update newer than 6 hours, don't update yet
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(nextScheduledUpdate);

			return false;
		}

		return true;
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		//should check null because in air plan mode it will be null
		return (netInfo != null && netInfo.isConnected());
	}
}
