package com.hasmobi.bgnovini.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hasmobi.bgnovini.NewsArticleThumbnailFinder;
import com.hasmobi.bgnovini.R;
import com.hasmobi.bgnovini.WebViewActivity;
import com.hasmobi.bgnovini.helpers.Constants;
import com.hasmobi.bgnovini.helpers.Typefaces;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.models.Source;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ViewHolder> {

	private List<NewsArticle> mDataset = new ArrayList<>();

	private Context mContext;

	public Tracker mTracker;

	Typeface tfTitles;

	public void setList(List<NewsArticle> list) {
		this.mDataset = list;
		notifyDataSetChanged();
	}

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends RecyclerView.ViewHolder {
		// each data item is just a string in this case
		public TextView title, description, date, source;
		public SimpleDraweeView ivThumbnail;
		public ImageView ivCheck, ivExpandArticle, ivShareArticle;

		public ViewHolder(View v) {
			super(v);
			title = (TextView) v.findViewById(R.id.tvTitle);
			description = (TextView) v.findViewById(R.id.tvDescription);
			date = (TextView) v.findViewById(R.id.tvDate);
			source = (TextView) v.findViewById(R.id.tvSource);
			ivThumbnail = (SimpleDraweeView) v.findViewById(R.id.ivThumbnail);
			ivCheck = (ImageView) v.findViewById(R.id.ivCheck);
			ivExpandArticle = (ImageView) v.findViewById(R.id.ivExpandArticle);
			ivShareArticle = (ImageView) v.findViewById(R.id.ivShareArticle);
		}
	}

	// Provide a suitable constructor (depends on the kind of dataset)
	public ArticlesAdapter(Context context, List<NewsArticle> myDataset) {
		this.mContext = context;
		this.mDataset = myDataset;
		this.tfTitles = Typefaces.get(context, "lora-bold");
	}

	// Create new views (invoked by the layout manager)
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent,
	                                     int viewType) {
		// create a new view
		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.single_news_article, parent, false);
		// set the view's size, margins, paddings and layout parameters
		return new ViewHolder(v);
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
		final NewsArticle article = mDataset.get(position);

		viewHolder.title.setText(article.getTitle());

		if (this.tfTitles != null) {
			viewHolder.title.setTypeface(this.tfTitles);
		}

		viewHolder.description.setText(article.getDescription());

		// Update the published date of the article, e.g. "5 minutes ago"
		CharSequence articleDateRelative = DateUtils.getRelativeTimeSpanString((article.getPubDate().getTime() > System.currentTimeMillis() ? System.currentTimeMillis() : article.getPubDate().getTime()), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
		viewHolder.date.setText(articleDateRelative);

		// Update the article source (newspaper) label
		final Source articleSource = article.getSource();
		articleSource.fetchFromLocalDatastoreInBackground(new GetCallback<ParseObject>() {
			@Override
			public void done(ParseObject object, ParseException e) {
				if (e != null) {
					e.printStackTrace();
					return;
				}
				Resources res = mContext.getResources();
				String label = res.getString(R.string.from) + " " + ((Source) object).getName();
				viewHolder.source.setText(label);
			}
		});

		// When the SHARE button is clicked, an app chooser is opened
		viewHolder.ivShareArticle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String url = article.getLink();
				Intent share = new Intent(android.content.Intent.ACTION_SEND);
				share.setType("text/plain");
				share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				share.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
				share.putExtra(Intent.EXTRA_TEXT, url);
				mContext.startActivity(Intent.createChooser(share, "Share"));

				// Track analytics event
				mTracker.send(new HitBuilders.EventBuilder()
						.setCategory(Constants.ANALYTICS_CATEGORY_ARTICLES)
						.setAction(Constants.ANALYTICS_ACTION_SHARE)
						.build());
			}
		});

		final View.OnClickListener _expandArticleClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				expandArticleAtPosition(position);

				// Track analytics event
				mTracker.send(new HitBuilders.EventBuilder()
						.setCategory(Constants.ANALYTICS_CATEGORY_ARTICLES)
						.setAction(Constants.ANALYTICS_ACTION_OPENED)
						.build());
			}
		};

		/**
		 * Open full activity in
		 * @see WebViewActivity
		 */
		viewHolder.ivExpandArticle.setOnClickListener(_expandArticleClickListener);
		viewHolder.title.setOnClickListener(_expandArticleClickListener);
		viewHolder.description.setOnClickListener(_expandArticleClickListener);
		viewHolder.ivThumbnail.setOnClickListener(_expandArticleClickListener);

		/**
		 * When the article is marked as "read" it disppears from
		 * the UI and is marked as read, so it doesn't appear in the
		 * list on future opens of the app
		 */
		viewHolder.ivCheck.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				article.setRead(true);
				article.pinInBackground(new SaveCallback() {
					@Override
					public void done(ParseException e) {
						// Update list, removing the item that was "read"
						mDataset.remove(position);
						notifyDataSetChanged();

						// Track analytics event
						mTracker.send(new HitBuilders.EventBuilder()
								.setCategory(Constants.ANALYTICS_CATEGORY_ARTICLES)
								.setAction(Constants.ANALYTICS_ACTION_ARCHIVE)
								.build());
					}
				});
			}
		});

		/**
		 * Load article thumbnail. Scrape it if it doesn't exist
		 */
		if (article.getThumbnailUrl() == null) {
			// Thumbnail was never scraped. Do it now
			Intent i = new Intent(mContext, NewsArticleThumbnailFinder.class);
			i.putExtra("link", article.getLink());
			mContext.startService(i);

			// hide the image while it loads
			viewHolder.ivThumbnail.setVisibility(View.GONE);
			Log.d(getClass().toString(), "Scraping fresh thumbnail for " + article.getLink());
		} else {
			if (article.getThumbnailUrl().equals("false")) {
				// Thumb scraping was attempted but we were unable to get a good image
				viewHolder.ivThumbnail.setVisibility(View.GONE);
				Log.d(getClass().toString(), "Article thumbnail not present for article " + article.getLink());
			} else {
				// Log.d(getClass().getSimpleName(), "Loading thumbnail " + article.getThumbnailUrl());
				Uri thumbUri = Uri.parse(article.getThumbnailUrl());
				viewHolder.ivThumbnail.setImageURI(thumbUri);

				viewHolder.ivThumbnail.setVisibility(View.VISIBLE);

			}
		}
	}

	private void expandArticleAtPosition(int position) {
		final NewsArticle article = this.mDataset.get(position);
		String url = article.getLink();
		Intent i = new Intent(mContext, WebViewActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra("url", url);
		mContext.startActivity(i);
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return mDataset.size();
	}
}