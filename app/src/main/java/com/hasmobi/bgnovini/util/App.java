package com.hasmobi.bgnovini.util;

import android.app.Application;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		Parse.enableLocalDatastore(getApplicationContext());

		Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

		ParseObject.registerSubclass(FavoriteSource.class);
		ParseObject.registerSubclass(Source.class);
		ParseObject.registerSubclass(NewsArticle.class);

		Parse.initialize(getApplicationContext(), "u2GANc30nVAXDWrRN2wVb1AvnfhtGi99aX4ZKbYC", "HlOCcz5I5EFhb0pfILHGoHuT9IOj4m2kHRnLxe9s");

		ParseUser.enableAutomaticUser();
		ParseUser.getCurrentUser().saveInBackground();

		ParseACL defaultACL = new ParseACL();
		ParseACL.setDefaultACL(defaultACL, true);

		// Create default options which will be used for every
		//  displayImage(...) call if no options will be passed to this method
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true).build();

		// Create global configuration and initialize ImageLoader with this config
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
				.diskCacheSize(20000000) // 20mb
				.memoryCacheSizePercentage(15)
				.defaultDisplayImageOptions(defaultOptions).build();
		ImageLoader.getInstance().init(config);

	}
}
