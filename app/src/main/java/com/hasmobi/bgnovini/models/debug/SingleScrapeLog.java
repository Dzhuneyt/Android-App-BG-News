package com.hasmobi.bgnovini.models.debug;

import com.hasmobi.bgnovini.models.Source;
import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("SingleScrapeLog")
public class SingleScrapeLog extends ParseObject {

	public SingleScrapeLog() {

	}

	public void setSource(Source source) {
		put("source", source);
	}

	public Source getSource() {
		return (Source) getParseObject("source");
	}
}
