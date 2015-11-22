package com.hasmobi.bgnovini.helpers;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class GenericThumbnailFetcher implements IThumbnailFetcher {
	@Override
	public String getThumbnailUrl(String pageUrl) {
		Document doc;
		try {
			doc = Jsoup.connect(pageUrl).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0").get();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (doc != null) {
			final Elements elements = doc.getElementsByAttributeValue("property", "og:image");
			final Element element = elements.first();

			if (element != null) {
				String thumb = element.attr("content");

				if (thumb != null) {

					Log.d(getClass().getSimpleName(), "Found thumbnail: " + thumb);

					return thumb;
				}
			}
		}

		return null;
	}

	static public String getHtml(String link) throws IOException {
		try {
			URL url = new URL(link);
			URLConnection con = null;
			con = url.openConnection();
			String encoding = con.getContentEncoding();
			if (encoding == null) {
				encoding = "ISO-8859-1";
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), encoding));
			StringBuilder sb = new StringBuilder();
			try {
				String s;
				while ((s = r.readLine()) != null) {
					sb.append(s);
					sb.append("\n");
				}
			} finally {
				r.close();
			}
			return sb.toString();
		} catch (IOException ex) {
			return "";
		}
	}
}
