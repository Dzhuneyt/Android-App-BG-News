package com.hasmobi.bgnovini;

import android.app.IntentService;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.util.TimeUtils;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class NewsUpdaterService extends IntentService {

	static public String BROADCAST_NEWS_UPDATED = "broadcast_news_updated";

	public NewsUpdaterService() {
		super("NewsUpdaterService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(getClass().getSimpleName(), "Starting RSS updater");

		showNotification();

		deleteOldJunk();

		ParseQuery<NewsArticle> pQueryExists = ParseQuery.getQuery(NewsArticle.class);
		pQueryExists.fromLocalDatastore();
		int existingItemsCount = 0;
		try {
			existingItemsCount = pQueryExists.count();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		boolean forceUpdateNow = intent.getBooleanExtra("force", false);

		long lastUpdateTimestamp = getSharedPreferences("updates", MODE_PRIVATE).getLong("last_update", -1);

		long nextScheduledUpdate = lastUpdateTimestamp + TimeUnit.HOURS.toMillis(6);
		if (nextScheduledUpdate > System.currentTimeMillis() && existingItemsCount > 0 && !forceUpdateNow) {
			// Last update newer than 6 hours, don't update yet
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(nextScheduledUpdate);

			Log.d(getClass().getSimpleName(), "No need to update yet. Next scheduled update at: " + c.getTime().toString());
			return;
		}

		final long msAtStart = System.currentTimeMillis();

		final ParseQuery<FavoriteSource> qFavoriteSources = ParseQuery.getQuery(FavoriteSource.class);
		qFavoriteSources.whereEqualTo("owner", ParseUser.getCurrentUser());
		qFavoriteSources.fromLocalDatastore();
		List<FavoriteSource> favoriteSources = null;
		try {
			favoriteSources = qFavoriteSources.find();
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
				Log.e(getClass().getSimpleName(), "Can not get source model for favorite", e);
				continue;
			}

			Log.d(getClass().getSimpleName(), "Scraping news from source: " + source.getName());

			URL url = null;
			try {
				url = new URL(source.getRssUrl());
			} catch (MalformedURLException e) {
				Log.e(getClass().getSimpleName(), "Invalid RSS URL", e);
				continue;
			}

			RssFeed feed = null;
			try {
				feed = RssReader.read(url);
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "Can not parse RSS", e);
				continue;
			}

			if (feed != null) {
				final ArrayList<RssItem> rssItems = feed.getRssItems();

				for (RssItem rssItem : rssItems) {
					final ParseQuery<NewsArticle> pqExists = ParseQuery.getQuery(NewsArticle.class);
					pqExists.fromLocalDatastore();
					pqExists.whereEqualTo("source", source);
					pqExists.whereEqualTo("link", rssItem.getLink());
					NewsArticle exists = null;
					try {
						exists = pqExists.getFirst();

						Log.d(getClass().getSimpleName(), "News article already exists and will be skipped for " + rssItem.getLink());
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
						try {
							newsArticle.pin();
						} catch (ParseException e) {
							Log.e(getClass().getSimpleName(), "Can not pin news article for " + rssItem.getLink(), e);
							continue;
						}

						Intent i = new Intent(this, NewsArticleThumbnailFinder.class);
						i.putExtra("link", rssItem.getLink());
						//startService(i);
					}
				}
			} else {
				Log.e(getClass().getSimpleName(), "Feed is null");
			}

			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_NEWS_UPDATED));
		}

		long msAtEnd = System.currentTimeMillis();
		long msToScrape = msAtEnd - msAtStart;

		Log.d(getClass().toString(), "Scraping completed in " + (msToScrape / 1000) + " seconds");

		getSharedPreferences("updates", MODE_PRIVATE).edit().putLong("last_update", System.currentTimeMillis()).apply();
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		super.onDestroy();
	}

	private void showNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle("Updating news");
		mBuilder.setOngoing(true).setProgress(100, 0, true);
		startForeground(1, mBuilder.build());
	}

	private void deleteOldJunk() {
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
