package com.hasmobi.bgnovini;

import android.content.res.Resources;
import android.support.v4.app.Fragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.hasmobi.bgnovini.models.debug.SingleScrapeLog;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class Application extends android.app.Application {

	private Tracker mTracker;

	@Override
	public void onCreate() {
		super.onCreate();

		Parse.enableLocalDatastore(getApplicationContext());

		Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

		ParseObject.registerSubclass(FavoriteSource.class);
		ParseObject.registerSubclass(Source.class);
		ParseObject.registerSubclass(NewsArticle.class);

		// Debugging helpers
		ParseObject.registerSubclass(SingleScrapeLog.class);

		Resources res = getResources();
		Parse.initialize(getApplicationContext(), res.getString(R.string.parse_key_1), res.getString(R.string.parse_key_2));

		ParseInstallation.getCurrentInstallation().saveInBackground();

		ParseUser.enableAutomaticUser();
		ParseUser.getCurrentUser().saveInBackground();

		ParseACL defaultACL = new ParseACL();
		ParseACL.setDefaultACL(defaultACL, true);

		Fresco.initialize(this);
	}

	/**
	 * Gets the default {@link Tracker} for this {@link Application}.
	 *
	 * @return tracker
	 */
	synchronized public Tracker getDefaultTracker() {
		if (mTracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
			mTracker = analytics.newTracker(R.xml.global_tracker);
			mTracker.enableExceptionReporting(true);
		}
		return mTracker;
	}

	static public void logFragmentScreenName(Fragment fragment) {
		Application app = fragment.getActivity() != null ? ((Application) fragment.getActivity().getApplication()) : null;
		if (app != null) {
			Tracker mTracker = app.getDefaultTracker();
			if (mTracker != null) {
				mTracker.setScreenName(fragment.getClass().getSimpleName());
				mTracker.send(new HitBuilders.ScreenViewBuilder().build());
			}
		}
	}
}
