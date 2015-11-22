package com.hasmobi.bgnovini;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hasmobi.bgnovini.helpers.Constants;
import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.ParseException;
import com.parse.ParseQuery;

import org.jsoup.Jsoup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import saxrssreader.RssFeed;
import saxrssreader.RssItem;
import saxrssreader.RssReader;

public class NewsUpdaterService extends IntentService {

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		NewsUpdaterService getService() {
			// Return this instance of LocalService so clients can call public methods
			return NewsUpdaterService.this;
		}
	}

	static public String BROADCAST_NEWS_UPDATED = "broadcast_news_updated";

	public NewsUpdaterService() {
		super("NewsUpdaterService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(getClass().getSimpleName(), "Starting RSS updater");

		final Tracker mTracker = ((Application) getApplication()).getDefaultTracker();

		showNotification();

		final long msAtStart = System.currentTimeMillis();

		final ParseQuery<FavoriteSource> q = ParseQuery.getQuery(FavoriteSource.class);
		q.fromLocalDatastore();
		List<FavoriteSource> favoriteSources = null;
		try {
			favoriteSources = q.find();
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}

		Log.d(getClass().getSimpleName(), "Scraping news from " + favoriteSources.size() + " sources");

		for (FavoriteSource favorite : favoriteSources) {
			Source source = null;
			try {
				favorite.getSource().fetchFromLocalDatastore();
				source = favorite.getSource();
			} catch (ParseException e) {
				mTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Can not get source model for favorite").setFatal(true).build());
				Log.e(getClass().getSimpleName(), "Can not get source model for favorite", e);
				continue;
			}

			Log.d(getClass().getSimpleName(), "Scraping news from source: " + source.getName());

			URL url = null;
			try {
				url = new URL(source.getRssUrl());
			} catch (MalformedURLException e) {
				Log.e(getClass().getSimpleName(), "Invalid RSS URL", e);
				mTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Invalid RSS URL " + source.getRssUrl()).setFatal(false).build());
				continue;
			}

			RssFeed feed = null;
			try {
				feed = RssReader.read(url);
			} catch (Exception e) {
				mTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Can not parse RSS feed of " + source.getRssUrl()).setFatal(false).build());
				continue;
			}

			if (feed == null) {
				Log.e(getClass().getSimpleName(), "Feed is null");
				continue;
			}

			final ArrayList<RssItem> rssItems = feed.getRssItems();

			if (rssItems == null || rssItems.size() == 0) {
				Log.d(getClass().toString(), "No items in feed");
				continue;
			}

			int existingCnt = 0;
			int createdCnt = 0;

			for (RssItem rssItem : rssItems) {
				final ParseQuery<NewsArticle> pqExists = ParseQuery.getQuery(NewsArticle.class);
				pqExists.fromLocalDatastore();
				pqExists.whereEqualTo("source", source);
				pqExists.whereEqualTo("link", rssItem.getLink());
				NewsArticle exists = null;
				try {
					exists = pqExists.getFirst();
				} catch (ParseException e) {
					Log.d(getClass().getSimpleName(), "Can not find existing news article for " + rssItem.getLink() + ". All good.");
				}

				if (exists == null) {
					NewsArticle newsArticle = new NewsArticle();
					if (rssItem.getLink() != null)
						newsArticle.setLink(rssItem.getLink());
					if (rssItem.getTitle() != null)
						newsArticle.setTitle(Jsoup.parse(rssItem.getTitle()).text());
					if (rssItem.getContent() != null)
						newsArticle.setContent(rssItem.getContent());
					if (rssItem.getDescription() != null)
						newsArticle.setDescription(Jsoup.parse(rssItem.getDescription()).text());
					if (rssItem.getPubDate() != null)
						newsArticle.setPubDate(rssItem.getPubDate());
					newsArticle.setSource(source);
					newsArticle.setRead(false);
					try {
						newsArticle.pin();
						createdCnt++;
					} catch (ParseException e) {
						Log.e(getClass().getSimpleName(), "Can not pin news article for " + rssItem.getLink(), e);
						mTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Can not pin news article for " + rssItem.getLink()).setFatal(true).build());
						continue;
					}
				} else {
					existingCnt++;
				}
			}

			if (existingCnt > 0) {
				Log.d(getClass().toString(), "Skipped " + existingCnt + " duplicate articles from " + source.getName());
			}
			if (createdCnt > 0) {
				Log.d(getClass().toString(), "Scraped " + createdCnt + " articles from " + source.getName());
			}

			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_NEWS_UPDATED));
		}

		long msAtEnd = System.currentTimeMillis();
		long msToScrape = msAtEnd - msAtStart;

		// Track analytics event timing (how much time it took to scrape new)
		mTracker.send(new HitBuilders.TimingBuilder()
				.setCategory(Constants.ANALYTICS_TIMING_CATEGORY_ARTICLES)
				.setVariable(Constants.ANALYTICS_TIMING_NAME_SCRAPE)
				.setValue(msToScrape)
				.build());

		Log.d(getClass().toString(), "Scraping completed in " + (msToScrape / 1000) + " seconds");

		getSharedPreferences("updates", MODE_PRIVATE).edit().putLong("last_update", System.currentTimeMillis()).apply();
	}

	@Override
	public void onDestroy() {
		startService(new Intent(getBaseContext(), JunkDeleterService.class));

		stopForeground(true);
		super.onDestroy();
	}

	private void showNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle(getResources().getString(R.string.updating_news));
		mBuilder.setOngoing(true).setProgress(100, 0, true);
		startForeground(1, mBuilder.build());
	}
}
