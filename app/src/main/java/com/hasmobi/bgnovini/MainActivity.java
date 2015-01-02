package com.hasmobi.bgnovini;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;


public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ParseAnalytics.trackAppOpenedInBackground(getIntent());

		if (savedInstanceState == null) {
			final ParseQuery<FavoriteSource> favoriteSourceParseQuery = ParseQuery.getQuery(FavoriteSource.class);
			favoriteSourceParseQuery.fromLocalDatastore();
			favoriteSourceParseQuery.findInBackground(new FindCallback<FavoriteSource>() {
				@Override
				public void done(List<FavoriteSource> favoriteSources, ParseException e) {
					if (e != null) {
						e.printStackTrace();
						return;
					}

					if (favoriteSources.size() == 0) {
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.container, new FragmentSources())
								.commit();
					} else {
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.container, new FragmentNews())
								.commit();
					}
				}
			});


		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
