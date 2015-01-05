package com.hasmobi.bgnovini;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.util.App;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;


public class MainActivity extends ActionBarActivity {

	//http://www.freepik.com/free-vector/retro-newspaper-illustration_725027.htm

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ParseAnalytics.trackAppOpenedInBackground(getIntent());

		if (savedInstanceState == null) {
			initActivity();
		}
	}

	private void initActivity() {
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

	BroadcastReceiver brInternetListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!App.isNetworkAvailable(context)) {
				Fragment exists = getSupportFragmentManager().findFragmentByTag("no_internet");
				if (exists != null) {
					getSupportFragmentManager().beginTransaction().remove(exists).commit();
				}

				DialogNoInternet dNoInternet = new DialogNoInternet();
				dNoInternet.show(getSupportFragmentManager(), "no_internet");
			} else {
				Fragment exists = getSupportFragmentManager().findFragmentByTag("no_internet");
				if (exists != null) {
					getSupportFragmentManager().beginTransaction().remove(exists).commit();
				}
				initActivity();
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(brInternetListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(brInternetListener);
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

	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			if (getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("rated", false)) {
				super.onBackPressed();
			} else {
				RemindRateDialog dRemindRate = new RemindRateDialog();
				dRemindRate.show(getSupportFragmentManager(), "remind_rate");
			}
		} else {
			super.onBackPressed();
		}
	}
}
