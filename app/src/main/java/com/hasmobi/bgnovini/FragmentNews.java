package com.hasmobi.bgnovini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.Tracker;
import com.hasmobi.bgnovini.adapters.ArticlesAdapter;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.SpacesItemDecoration;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

public class FragmentNews extends Fragment {

	private Context context;

	private Tracker mTracker;

	private BroadcastReceiver brRefreshNewsFromCache = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.d(getClass().toString(), "Broadcast received");
			refreshNewsFromCache();
		}
	};
	private BroadcastReceiver brThumbnailsUpdated = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.d(getClass().toString(), "New thumbnails. Updating list of articles");
			if (rvNews != null) {
				rvNews.getAdapter().notifyDataSetChanged();
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(brRefreshNewsFromCache, new IntentFilter(NewsUpdaterService.BROADCAST_NEWS_UPDATED));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(brThumbnailsUpdated, new IntentFilter(NewsArticleThumbnailFinder.BROADCAST_THUMBNAIL_DOWNLOADED));

		// Track analytics
		mTracker = getActivity() != null ? ((Application) getActivity().getApplication()).getDefaultTracker() : null;
		if (mTracker != null) {
			mTracker.setScreenName(getClass().getSimpleName());
		}
		Application.logFragmentScreenName(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(brRefreshNewsFromCache);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(brThumbnailsUpdated);
	}

	private void refreshNewsFromCache() {
		Log.d(getClass().toString(), "Refreshing news from cache");
		ParseQuery<NewsArticle> qNews = ParseQuery.getQuery(NewsArticle.class);
		qNews.fromLocalDatastore();
		qNews.whereEqualTo("read", false);
		qNews.addDescendingOrder("date"); // newest first
		qNews.findInBackground(new FindCallback<NewsArticle>() {
			@Override
			public void done(List<NewsArticle> localNewsArticles, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}
				Log.d(getClass().toString(), "Found " + localNewsArticles.size() + " articles");
				setNewArticleList(localNewsArticles);
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_news, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.d(getClass().toString(), "onViewCreated");

		getContext().startService(new Intent(getContext(), NewsUpdaterService.class));

		RecyclerView rvNews = (RecyclerView) view.findViewById(R.id.rvNews);
		rvNews.setHasFixedSize(true);

		int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.articles_grid_margin);
		rvNews.addItemDecoration(new SpacesItemDecoration(spacingInPixels));

		StaggeredGridLayoutManager mLayoutManager;
		int staggeredLayoutColumns = getResources().getInteger(R.integer.swagger_columns_count);
		mLayoutManager = new StaggeredGridLayoutManager(staggeredLayoutColumns, StaggeredGridLayoutManager.VERTICAL);

		rvNews.setLayoutManager(mLayoutManager);

		// Load articles for the first time
		refreshNewsFromCache();
	}

	private RecyclerView rvNews;
	private ArticlesAdapter adapter;

	private void setNewArticleList(List<NewsArticle> objects) {
		if (getView() == null) {
			Log.d(getClass().toString(), "Can not find view");
			return;
		}

		rvNews = (RecyclerView) getView().findViewById(R.id.rvNews);

		if (adapter == null) {
			adapter = new ArticlesAdapter(getContext(), objects);
			adapter.mTracker = mTracker;
			rvNews.setAdapter(adapter);
		} else {
			adapter.setList(objects);
		}
	}
}
