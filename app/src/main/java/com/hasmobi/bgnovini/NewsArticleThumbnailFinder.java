package com.hasmobi.bgnovini;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hasmobi.bgnovini.models.NewsArticle;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

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

		Log.d(getClass().getSimpleName(), "Scraping thumbnail for " + link);

		Document doc;
		try {
			doc = Jsoup.connect(link).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0").get();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		ParseQuery<NewsArticle> q = ParseQuery.getQuery(NewsArticle.class);
		q.fromLocalDatastore();
		q.whereEqualTo("link", link);

		List<NewsArticle> articles = null;


		try {
			articles = q.find();
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}

		if (articles == null) {
			return;
		}

		if (doc != null) {
			final Elements elements = doc.getElementsByAttributeValue("property", "og:image");
			final Element element = elements.first();

			if (element != null) {
				String thumb = element.attr("content");

				if (thumb != null) {

					Log.d(getClass().getSimpleName(), "Found thumbnail: " + thumb);

					try {

						for (NewsArticle singleArticle : articles) {
							singleArticle.setThumbnailUrl(thumb);
						}

						ParseObject.pinAll(articles);

						LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_THUMBNAIL_DOWNLOADED));

						return;

					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		}

		Log.e(getClass().getSimpleName(), "Can not find thumbnail for " + link);

		for (NewsArticle singleArticle : articles) {
			singleArticle.setThumbnailUrl("false");
		}

		try {
			ParseObject.pinAll(articles);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_THUMBNAIL_DOWNLOADED));
	}
}
