package com.hasmobi.bgnovini.helpers;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class NoviniBgFetcher implements IThumbnailFetcher {

	@Override
	public String getThumbnailUrl(String pageUrl) {
		Document doc;
		try {
			doc = Jsoup.connect(pageUrl).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0").get();
		} catch (IOException e) {
			Log.e(getClass().toString(), "Can not reach URL " + pageUrl, e);
			e.printStackTrace();
			return null;
		}

		if (doc != null) {
			final Elements elements = doc.getElementsByAttributeValue("property", "og:image");
			final Element element = elements.first();

			if (element != null) {
				Log.d(getClass().toString(), "element found");
				String thumb = element.attr("content");

				if (thumb != null) {

					Log.d(getClass().getSimpleName(), "Found thumbnail: " + thumb);

					return thumb;
				}
			}
		}

		return null;
	}
}
