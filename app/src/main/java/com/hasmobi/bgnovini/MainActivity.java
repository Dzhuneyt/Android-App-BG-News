package com.hasmobi.bgnovini;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.hasmobi.bgnovini.adapters.SourcesAdapter;
import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.Source;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private NewsUpdaterService mBoundService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		updateDrawer();

		setupMainFragment();

		// If there are no favorite sources on first open
		// Immediately open the drawer to "teach" the user
		// that he can find them there
		final ParseQuery<FavoriteSource> qFavoriteSources = FavoriteSource.getQuery();
		qFavoriteSources.fromLocalDatastore();
		qFavoriteSources.countInBackground(new CountCallback() {
			@Override
			public void done(int count, ParseException e) {
				if (count <= 0) {
					if (!drawer.isDrawerOpen(GravityCompat.START)) {
						drawer.openDrawer(GravityCompat.START);
					}
				}
			}
		});

	}

	private void setupMainFragment() {
		getSupportFragmentManager().beginTransaction().replace(R.id.llContainer, new FragmentNews()).commit();
	}

	private void updateDrawer() {
		final ParseQuery<Source> qSources = ParseQuery.getQuery(Source.class);
		qSources.addAscendingOrder("name");
		qSources.findInBackground(new FindCallback<Source>() {
			@Override
			public void done(List<Source> liveSourcesList, ParseException e) {
				if (e != null) {
					e.printStackTrace();

					qSources.fromLocalDatastore();
					qSources.findInBackground(new FindCallback<Source>() {
						@Override
						public void done(List<Source> cachedSources, ParseException e) {
							if (e != null) {
								e.printStackTrace();
							} else {
								setNewSources(cachedSources);
							}
						}
					});
				} else {
					setNewSources(liveSourcesList);
				}
			}

		});
	}

	private void setNewSources(List<Source> sources) {
		final ListView lvNavDrawer = (ListView) findViewById(R.id.lvNavDrawer);
		SourcesAdapter adapter = new SourcesAdapter(getBaseContext(), sources);
		lvNavDrawer.setAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_camera) {
			// Handle the camera action
		} else if (id == R.id.nav_gallery) {

		} else if (id == R.id.nav_slideshow) {

		} else if (id == R.id.nav_manage) {

		} else if (id == R.id.nav_share) {

		} else if (id == R.id.nav_send) {

		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}
}
