package com.hasmobi.bgnovini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.util.App;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FragmentNews extends Fragment {

	Context context;

	private BroadcastReceiver brRefreshNewsFromCache = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshNewsFromCache();
		}
	};
	private BroadcastReceiver brRedrawThumbnails = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (getView() == null) return;

			final ListView lv = (ListView) getView().findViewById(R.id.lv);

			if (lv == null) return;

			NewsAdapter adapter = (NewsAdapter) lv.getAdapter();

			if (adapter == null) return;

			// save index and top position
			int index = lv.getFirstVisiblePosition();
			View v = lv.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();

			adapter.notifyDataSetChanged();

			// restore index and position
			lv.setSelectionFromTop(index, top);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();

		context = getActivity().getBaseContext();

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(brRefreshNewsFromCache, new IntentFilter(NewsUpdaterService.BROADCAST_NEWS_UPDATED));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(brRedrawThumbnails, new IntentFilter(NewsArticleThumbnailFinder.BROADCAST_THUMBNAIL_DOWNLOADED));
	}

	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(brRefreshNewsFromCache);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(brRedrawThumbnails);

		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_news, container, false);
	}

	@Override
	public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		context = getActivity().getBaseContext();

		final SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl);
		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Toast.makeText(context, getResources().getString(R.string.updating_news), Toast.LENGTH_SHORT).show();
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().remove("last_update").commit();
						context.startService(new Intent(context, NewsUpdaterService.class));
						mSwipeRefreshLayout.setRefreshing(false);
					}
				}, 2000);
			}
		});

		ParseQuery<NewsArticle> pQueryExists = ParseQuery.getQuery(NewsArticle.class);
		pQueryExists.countInBackground(new CountCallback() {
			@Override
			public void done(int count, ParseException e) {
				if (e != null || count == 0) {
					// No cached news yet. Force refresh now
					context.startService(new Intent(context, NewsUpdaterService.class));
				} else {
					// There are some cached news. Check if the last
					// update is too old (older than 6 hours) and refresh
					if (App.isNewsNeedsUpdating(context)) {
						context.startService(new Intent(context, NewsUpdaterService.class));
					}
				}
			}
		});

		refreshNewsFromCache();
	}

	private void refreshNewsFromCache() {
		ParseQuery<NewsArticle> qNews = ParseQuery.getQuery(NewsArticle.class);
		qNews.fromLocalDatastore();
		qNews.addDescendingOrder("date"); // newest first
		qNews.findInBackground(new FindCallback<NewsArticle>() {
			@Override
			public void done(List<NewsArticle> localNewsArticles, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}

				if (getView() == null) {
					Log.e(getClass().toString(), "View not ready yet. Can not refresh ListView");
					return;
				}

				NewsAdapter adapter = new NewsAdapter(context, localNewsArticles);

				final ListView lv = (ListView) getView().findViewById(R.id.lv);

				// save index and top position
				int index = lv.getFirstVisiblePosition();
				View v = lv.getChildAt(0);
				int top = (v == null) ? 0 : v.getTop();

				lv.setAdapter(adapter);

				// restore index and position
				lv.setSelectionFromTop(index, top);
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_news_feed, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_edit_sources:
				getActivity().getSupportFragmentManager().beginTransaction()
						.replace(R.id.container, new FragmentSources())
						.commit();
				return true;
			case R.id.action_refresh:
				context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().remove("last_update").commit();
				context.startService(new Intent(context, NewsUpdaterService.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
