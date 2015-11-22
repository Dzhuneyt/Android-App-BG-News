package com.hasmobi.bgnovini.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;

@ParseClassName("NewsArticle")
public class NewsArticle extends ParseObject {

	public NewsArticle() {
	}

	public static ParseQuery<NewsArticle> getQuery() {
		return ParseQuery.getQuery(NewsArticle.class);
	}

	public String getTitle() {
		return getString("title");
	}

	public void setTitle(String title) {
		put("title", title);
	}

	public void setSource(Source source) {
		put("source", source);
	}

	public Source getSource() {
		return (Source) getParseObject("source");
	}

	public void setLink(String link) {
		put("link", link);
	}

	public String getLink() {
		return getString("link");
	}

	public void setDescription(String description) {
		put("description", description);
	}

	public String getDescription() {
		return getString("description");
	}

	public void setPubDate(Date pubDate) {
		put("date", pubDate);
	}

	public Date getPubDate() {
		return getDate("date");
	}

	public void setContent(String content) {
		put("content", content);
	}

	public String getContent() {
		return getString("content");
	}

	public void setThumbnailUrl(String thumb) {
		put("thumb", thumb);
	}

	public String getThumbnailUrl() {
		return getString("thumb");
	}

	public void setRead(boolean read) {
		put("read", read);
	}

	public boolean getRead() {
		return getBoolean("read");
	}
}
