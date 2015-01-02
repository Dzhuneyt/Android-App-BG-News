package com.hasmobi.bgnovini.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("Source")
public class Source extends ParseObject {

	public Source() {
	}

	public String getName() {
		return getString("name");
	}

	public String getRssUrl() {
		return getString("rss");
	}
}
