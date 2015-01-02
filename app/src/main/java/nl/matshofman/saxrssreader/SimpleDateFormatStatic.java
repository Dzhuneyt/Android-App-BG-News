package nl.matshofman.saxrssreader;

import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SimpleDateFormatStatic {

	static Map<String, SimpleDateFormat> map = new HashMap<>();

	public static SimpleDateFormat getCachedDateObject(String format) {
		if (map.get(format) != null) {
			return map.get(format);
		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
			map.put(format, dateFormat);
			return dateFormat;
		}
	}
}
