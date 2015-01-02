package com.hasmobi.bgnovini;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.hasmobi.bgnovini.models.FavoriteSource;
import com.hasmobi.bgnovini.models.Source;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class SourcesAdapter extends BaseAdapter {

	List<Source> items = new ArrayList<>();
	List<FavoriteSource> favoriteSources = new ArrayList<>();
	Context context = null;

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
		final Source item = getItem(position);

		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.single_source, parent, false);

		TextView tvTitle = (TextView) v.findViewById(R.id.tvTitle);
		final Button bAdd = (Button) v.findViewById(R.id.bAdd);

		tvTitle.setText(item.getName());

		final ParseQuery<FavoriteSource> qAlreadyAdded = ParseQuery.getQuery(FavoriteSource.class);
		qAlreadyAdded.whereEqualTo("owner", ParseUser.getCurrentUser());
		qAlreadyAdded.whereEqualTo("source", item);
		qAlreadyAdded.fromLocalDatastore();

		boolean found = false;
		for (FavoriteSource favorite : favoriteSources) {
			if (favorite.getSource().getObjectId().equals(item.getObjectId())) {
				// Source already present in favorites
				bAdd.setText(context.getResources().getString(R.string.remove));
				found = true;
				break;
			}
		}
		if (!found) {
			bAdd.setText(context.getResources().getString(R.string.add));
		}

		bAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				v.setEnabled(false);

				qAlreadyAdded.getFirstInBackground(new GetCallback<FavoriteSource>() {
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
							s.setSource(item);
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
