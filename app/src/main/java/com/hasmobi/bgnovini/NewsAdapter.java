package com.hasmobi.bgnovini;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.hasmobi.bgnovini.models.NewsArticle;
import com.hasmobi.bgnovini.util.Typefaces;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends BaseAdapter {

	List<NewsArticle> items = new ArrayList<>();
	Context context = null;

	public NewsAdapter(Context baseContext, List<NewsArticle> newsArticles) {
		this.context = baseContext;
		this.items = newsArticles;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public NewsArticle getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final NewsArticle item = getItem(position);

		View rowView = convertView;

		if (rowView == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			rowView = inflater.inflate(R.layout.single_news_article, parent, false);

			ViewHolder viewHolder = new ViewHolder();

			viewHolder.tvTitle = (TextView) rowView.findViewById(R.id.tvTitle);
			//viewHolder.titleHolder = (RelativeLayout) rowView.findViewById(R.id.rlTitleHolder);
			viewHolder.tvDescr = (TextView) rowView.findViewById(R.id.tvDescription);
			viewHolder.tvDate = (TextView) rowView.findViewById(R.id.tvDate);
			viewHolder.tvSourceName = (TextView) rowView.findViewById(R.id.tvSourceName);
			viewHolder.image = (SimpleDraweeView) rowView.findViewById(R.id.iv);
			viewHolder.fullWrapper = (RelativeLayout) rowView.findViewById(R.id.rlHolder);

			Typeface typefaceTitle = Typefaces.get(context, "playfairdisplay.ttf");
			Typeface typefaceBody = Typefaces.get(context, "opensans-regular.ttf");
			viewHolder.tvTitle.setTypeface(typefaceTitle);
			viewHolder.tvDescr.setTypeface(typefaceBody);
			rowView.setTag(viewHolder);
		}

		final ViewHolder holder = (ViewHolder) rowView.getTag();

		holder.tvTitle.setText(item.getTitle());
		holder.tvDescr.setText(item.getDescription());

		if (!item.getSource().isDataAvailable()) {
			Log.d(getClass().toString(), "Fetching local source");
			item.getSource().fetchFromLocalDatastoreInBackground(new GetCallback<ParseObject>() {
				@Override
				public void done(ParseObject parseObject, ParseException e) {
					holder.tvSourceName.setText(item.getSource().getName());
				}
			});
		} else {
			holder.tvSourceName.setText(item.getSource().getName());
		}

		CharSequence relative;
		if (item.getPubDate().getTime() > System.currentTimeMillis()) {
			// Can not be in the future
			relative = DateUtils.getRelativeTimeSpanString(System.currentTimeMillis(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
		} else {
			relative = DateUtils.getRelativeTimeSpanString(item.getPubDate().getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
		}

		holder.tvDate.setText(relative);

		if (item.getThumbnailUrl() != null) {
			if (item.getThumbnailUrl().equals("false")) {
				// Thumb scraping was attempted but we were unable to get a good image
				holder.image.setVisibility(View.GONE);
			} else {
				Log.d(getClass().getSimpleName(), "Loading thumbnail");
				Uri thumbUri = Uri.parse(item.getThumbnailUrl());
				holder.image.setImageURI(thumbUri);
				holder.image.setVisibility(View.VISIBLE);
			}

		} else {
			// Thumb scraping was never attempted. Try it now.
			Intent i = new Intent(context, NewsArticleThumbnailFinder.class);
			i.putExtra("link", item.getLink());
			context.startService(i);

			holder.image.setVisibility(View.GONE);
		}

		holder.fullWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String link = item.getLink();
				Intent i = new Intent(context, WebViewActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("url", link);
				context.startActivity(i);
			}
		});

		return rowView;
	}

	static class ViewHolder {
		public TextView tvTitle, tvDescr, tvSourceName, tvDate;
		public SimpleDraweeView image;
		public RelativeLayout fullWrapper;
		public RelativeLayout titleHolder;
	}
}
