package com.hasmobi.bgnovini;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.hasmobi.bgnovini.models.NewsArticle;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JunkDeleterService extends IntentService {

	public JunkDeleterService() {
		super("JunkDeleterService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Delete items older than 7 days
		ParseQuery<NewsArticle> pQueryOld = ParseQuery.getQuery(NewsArticle.class);
		Date dOlderThanThreshold = new Date();
		dOlderThanThreshold.setTime(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7));

		pQueryOld.fromLocalDatastore();
		pQueryOld.whereLessThanOrEqualTo("date", dOlderThanThreshold);
		try {
			List<NewsArticle> oldItems = pQueryOld.find();
			Log.d(getClass().getSimpleName(), "Deleting old objects count: " + oldItems.size());
			ParseObject.unpinAll(oldItems);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
