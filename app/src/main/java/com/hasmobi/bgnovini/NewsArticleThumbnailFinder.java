package com.hasmobi.bgnovini;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hasmobi.bgnovini.helpers.GenericThumbnailFetcher;
import com.hasmobi.bgnovini.helpers.IThumbnailFetcher;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.ParseException;
import com.parse.ParseQuery;

/**
 * A simple class to scrape the perfect thumbnail for a given
 * URL (news article) and save that thumbnail inside the
 * NewsArticle ParseObject
 */
public class NewsArticleThumbnailFinder extends IntentService {

	public static String BROADCAST_THUMBNAIL_DOWNLOADED = "thumb_updated";

	public NewsArticleThumbnailFinder() {
		super("NewsArticleThumbnailFinder");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String link = intent.getStringExtra("link");

		if (link == null) return;

		String thumbUrl = null;

		// Log.d(getClass().getSimpleName(), "Scraping thumbnail for " + link);

		ParseQuery<NewsArticle> q = ParseQuery.getQuery(NewsArticle.class);
		q.fromLocalDatastore();
		q.whereEqualTo("link", link);

		NewsArticle article = null;
		Source source = null;
		try {
			article = q.getFirst();
			if (article == null) {
				Log.d(getClass().toString(), "Article not found");
				return;
			} else {
				source = article.getSource();
				source.fetchFromLocalDatastore();
			}

			IThumbnailFetcher fetcher = null;
			if (source.getName().contains("Novini")) {
				// fetcher = new NoviniBgFetcher(); not ready yet
				fetcher = new GenericThumbnailFetcher();
			} else {
				fetcher = new GenericThumbnailFetcher();
			}

			thumbUrl = fetcher.getThumbnailUrl(article.getLink());

			if (thumbUrl != null) {
				article.setThumbnailUrl(thumbUrl);
				article.pin();
				LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_THUMBNAIL_DOWNLOADED));
				return;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		if (thumbUrl == null && article != null) {
			// Log.e(getClass().toString(), "Thumb not found. Saving false");
			article.setThumbnailUrl("false");
			try {
				article.pin();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
