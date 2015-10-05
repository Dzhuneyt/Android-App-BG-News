package com.hasmobi.bgnovini;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.Source;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class SourcesAdapter extends BaseAdapter {

	List<Source> items = new ArrayList<>();
	List<FavoriteSource> favoriteSources = new ArrayList<>();
	Context context = null;

	List<String> newFavoriteSources = new ArrayList<>();

	public SourcesAdapter(Context context, List<Source> originalList) {
		this.context = context;
		this.items = originalList;

		refreshFavorites();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Source getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		final View v = inflater.inflate(R.layout.single_source, parent, false);

		TextView tvTitle = (TextView) v.findViewById(R.id.tvTitle);
		final Button bAdd = (Button) v.findViewById(R.id.bAdd);
		final CheckBox cbAdd = (CheckBox) v.findViewById(R.id.cbAdd);

		final Source source = getItem(position);

		tvTitle.setText(source.getName());

		final ParseQuery<FavoriteSource> qFromDb = ParseQuery.getQuery(FavoriteSource.class);
		qFromDb.whereEqualTo("owner", ParseUser.getCurrentUser());
		qFromDb.whereEqualTo("source", source);
		qFromDb.fromLocalDatastore();

		boolean found = false;
		for (FavoriteSource favorite : favoriteSources) {
			if (favorite.getSource().getObjectId().equals(source.getObjectId())) {
				// Source already present in favorites
				cbAdd.setChecked(true);
				found = true;
				break;
			}
		}
		if (!found) {
			cbAdd.setChecked(false);
		}

		cbAdd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					final FavoriteSource newFavSource = new FavoriteSource();
					newFavSource.setSource(source);
					newFavSource.setOwner(ParseUser.getCurrentUser());
					newFavSource.pinInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							Log.d(getClass().toString(), "Added new favorite source: " + source.getName() + " - object ID: " + source.getObjectId());
							buttonView.setEnabled(true);
							refreshFavorites();
						}
					});
				} else {
					// Delete
					qFromDb.findInBackground(new FindCallback<FavoriteSource>() {
						@Override
						public void done(List<FavoriteSource> list, ParseException e) {
							if (list != null && list.size() > 0) {
								ParseObject.unpinAllInBackground(list, new DeleteCallback() {
									@Override
									public void done(ParseException e) {
										refreshFavorites();
										buttonView.setEnabled(true);
										Log.d(getClass().toString(), "Removed favorite source: " + source.getName() + " - object ID: " + source.getObjectId());
									}
								});
							}
						}
					});
				}
			}
		});
		bAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				v.setEnabled(false);

				qFromDb.getFirstInBackground(new GetCallback<FavoriteSource>() {
					@Override
					public void done(FavoriteSource favoriteSource, ParseException e) {
						if (e == null && favoriteSource != null) {
							// Item already added to favorites. Remove it now
							favoriteSource.unpinInBackground(new DeleteCallback() {
								@Override
								public void done(ParseException e) {
									refreshFavorites();

									v.setEnabled(true);
								}
							});

							bAdd.setText(context.getResources().getString(R.string.add));
						} else {
							FavoriteSource s = new FavoriteSource();
							s.setSource(source);
							s.setOwner(ParseUser.getCurrentUser());
							s.pinInBackground(new SaveCallback() {
								@Override
								public void done(ParseException e) {
									bAdd.setText(context.getResources().getString(R.string.remove));

									v.setEnabled(true);

									refreshFavorites();
								}
							});
						}
					}
				});
			}
		});

		return v;
	}

	private void refreshFavorites() {
		final ParseQuery<FavoriteSource> q = ParseQuery.getQuery(FavoriteSource.class);
		q.fromLocalDatastore();
		q.whereEqualTo("owner", ParseUser.getCurrentUser());
		q.findInBackground(new FindCallback<FavoriteSource>() {
			@Override
			public void done(List<FavoriteSource> list, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}

				favoriteSources = list;

				notifyDataSetChanged();
			}
		});
	}

}
