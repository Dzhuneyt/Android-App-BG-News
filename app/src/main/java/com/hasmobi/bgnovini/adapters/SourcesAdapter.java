package com.hasmobi.bgnovini.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.hasmobi.bgnovini.NewsUpdaterService;
import com.hasmobi.bgnovini.R;
import com.hasmobi.bgnovini.helpers.Typefaces;
import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class SourcesAdapter extends BaseAdapter {

	private Context mContext;
	private List<Source> sources = new ArrayList<>();

	private List<FavoriteSource> currentFavorites = new ArrayList<>();

	public SourcesAdapter(Context baseContext, List<Source> sources) {
		this.mContext = baseContext;
		this.sources = sources;

		refreshCurrentFavorites();

	}

	@Override
	public int getCount() {
		return sources.size();
	}

	@Override
	public Source getItem(int position) {
		return sources.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);

		final Source source = getItem(position);

		final CheckedTextView tv = (CheckedTextView) v;

		boolean alreadyInFavorites = false;
		for (FavoriteSource currentFav : currentFavorites) {
			if (currentFav.getSource().getObjectId().equals(source.getObjectId())) {
				alreadyInFavorites = true;
			}
		}

		if (alreadyInFavorites) {
			if (!tv.isChecked()) {
				tv.toggle();
			}
		} else {
			if (tv.isChecked()) {
				tv.toggle();
			}
		}


		tv.setText(source.getName());
		Typeface tf = Typefaces.get(mContext, "lora-regular");
		tv.setTypeface(tf);

		tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (tv.isChecked()) {
					// was checked, now is not. Delete old favorite source

					// First delete all articles cached from the (now unchecked) source
					ParseQuery<NewsArticle> qArticles = NewsArticle.getQuery();
					qArticles.whereEqualTo("source", source);
					qArticles.fromLocalDatastore();
					qArticles.findInBackground(new FindCallback<NewsArticle>() {
						@Override
						public void done(List<NewsArticle> objects, ParseException e) {
							ParseObject.unpinAllInBackground(objects);
						}
					});

					// Finally delete the source itself
					ParseQuery<FavoriteSource> qFavUnchecked = FavoriteSource.getQuery();
					qFavUnchecked.fromLocalDatastore();
					qFavUnchecked.whereEqualTo("source", source);
					qFavUnchecked.findInBackground(new FindCallback<FavoriteSource>() {
						@Override
						public void done(List<FavoriteSource> objects, ParseException e) {
							ParseObject.unpinAllInBackground(objects, new DeleteCallback() {
								@Override
								public void done(ParseException e) {
									Log.d(getClass().toString(), "Removed favorite source " + source.getName());

									refreshCurrentFavorites();

									mContext.startService(new Intent(mContext, NewsUpdaterService.class));

									Toast.makeText(mContext, mContext.getResources().getString(R.string.updating_news), Toast.LENGTH_SHORT).show();
								}
							});
						}
					});
				} else {
					Log.d(getClass().toString(), "Is not selected");

					ParseQuery<FavoriteSource> qFavUnchecked = FavoriteSource.getQuery();
					qFavUnchecked.fromLocalDatastore();
					qFavUnchecked.whereEqualTo("source", source);
					qFavUnchecked.findInBackground(new FindCallback<FavoriteSource>() {
						@Override
						public void done(List<FavoriteSource> objects, ParseException e) {
							ParseObject.deleteAllInBackground(objects, new DeleteCallback() {
								@Override
								public void done(ParseException e) {
									if (e != null) {
										e.printStackTrace();
										return;
									}

									FavoriteSource s = new FavoriteSource();
									s.setSource(source);
									s.pinInBackground(new SaveCallback() {
										@Override
										public void done(ParseException e) {
											refreshCurrentFavorites();

											mContext.startService(new Intent(mContext, NewsUpdaterService.class));

											Toast.makeText(mContext, mContext.getResources().getString(R.string.updating_news), Toast.LENGTH_SHORT).show();
										}
									});
								}
							});
						}
					});
				}
				tv.toggle();
			}
		});

		return v;
	}

	private void refreshCurrentFavorites() {
		ParseQuery<FavoriteSource> queryOldFavorites = FavoriteSource.getQuery();
		queryOldFavorites.fromLocalDatastore();
		queryOldFavorites.findInBackground(new FindCallback<FavoriteSource>() {
			@Override
			public void done(List<FavoriteSource> objects, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}

				currentFavorites = objects;
				notifyDataSetChanged();
			}
		});
	}

}
