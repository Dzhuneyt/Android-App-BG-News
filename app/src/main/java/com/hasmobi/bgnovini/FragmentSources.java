package com.hasmobi.bgnovini;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class FragmentSources extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_sources, container, false);
	}

	@Override
	public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		final Button bContinue = (Button) view.findViewById(R.id.bContinue);

		final ListView listView = (ListView) view.findViewById(R.id.lv);

		bContinue.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {


				// Delete cached news items from sources we do not have in favorites anymore
				ParseQuery<FavoriteSource> qNewFavorites = ParseQuery.getQuery(FavoriteSource.class);
				qNewFavorites.fromLocalDatastore();
				qNewFavorites.whereEqualTo("owner", ParseUser.getCurrentUser());
				qNewFavorites.findInBackground(new FindCallback<FavoriteSource>() {
					@Override
					public void done(List<FavoriteSource> list, ParseException e) {

						List<Source> sourceList = new ArrayList<>();
						for (FavoriteSource newFav : list) {
							sourceList.add(newFav.getSource());
						}

						ParseQuery<NewsArticle> pQueryArticlesNotFromFavorites = ParseQuery.getQuery(NewsArticle.class);
						pQueryArticlesNotFromFavorites.fromLocalDatastore();
						pQueryArticlesNotFromFavorites.whereNotContainedIn("source", sourceList);
						pQueryArticlesNotFromFavorites.findInBackground(new FindCallback<NewsArticle>() {
							@Override
							public void done(List<NewsArticle> newsArticles, ParseException e) {
								ParseObject.unpinAllInBackground(newsArticles, new DeleteCallback() {
									@Override
									public void done(ParseException e) {
										LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(NewsUpdaterService.BROADCAST_NEWS_UPDATED));
									}
								});
							}
						});

						if (list.size() > 0) {
							getActivity().getSupportFragmentManager().beginTransaction()
									.replace(R.id.container, new FragmentNews())
									.commit();
						} else {
							Toast.makeText(getActivity(), getResources().getString(R.string.choose_any_source), Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		});

		final ParseQuery<Source> qSources = ParseQuery.getQuery(Source.class);
		qSources.addAscendingOrder("name");
		qSources.findInBackground(new FindCallback<Source>() {
			@Override
			public void done(List<Source> sources, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}

				if (sources.size() == 0) {
					qSources.fromLocalDatastore();
					qSources.findInBackground(new FindCallback<Source>() {
						@Override
						public void done(List<Source> sources, ParseException e) {
							Log.d(getClass().toString(), "Received " + sources.size());
							SourcesAdapter adapter = new SourcesAdapter(getActivity().getBaseContext(), sources);

							listView.setAdapter(adapter);
						}
					});
				} else {
					Log.d(getClass().toString(), "Received " + sources.size());
					SourcesAdapter adapter = new SourcesAdapter(view.getContext(), sources);

					listView.setAdapter(adapter);
				}


			}
		});
	}
}
