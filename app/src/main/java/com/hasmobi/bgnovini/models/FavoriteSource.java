package com.hasmobi.bgnovini.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("FavoriteSource")
public class FavoriteSource extends ParseObject {

	public FavoriteSource() {
	}

	public void setOwner(ParseUser author) {
		put("owner", author);
	}

	public ParseUser getOwner() {
		return getParseUser("owner");
	}

	public void setSource(Source source) {
		put("source", source);
	}

	public Source getSource() {
		return (Source) getParseObject("source");
	}
}
