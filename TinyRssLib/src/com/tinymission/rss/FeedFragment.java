package com.tinymission.rss;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/** Base class for activities that show an RSS feed.
 *
 */
public abstract class FeedFragment extends Fragment {

	
	/** Subclasses can override to provide a different main list layout
	 * This layout must have a ListView with id android:id/list
	 * @return the list layout id
	 */	
	public int getListLayoutId() {
		return R.layout.feed;
	}
	
	/** Subclasses can override to provide a different item layout
	 * @return the feed item layout id
	 */	
	public int getItemLayoutId() {
		return R.layout.feed_list_item;
	}
	
	/**
	 * @return the maximum number of items to render, or less than 0 for all items (default)
	 */
	public int getMaxItems() {
		return -1;
	}
	
	/** Subclasses can override this to show an image next to the feed text.
	 * 
	 * @return true if an image should be shown next to the feed text (default false)
	 */
	public boolean isImageVisible() {
		return false;
	}
	
	/** Subclasses can override to make the item date visible under the title
	 * 
	 * @return true to show the item date under the title (default false)
	 */
	public boolean isDateVisible() {
		return false;
	}

    /** Subclasses can override to show the title, author, or both
     *
     * @return 0 to show title only, 1 to show author only, 2 to show both (default is 0)
     */
    public int showTitleAuthorOrBoth() {
        return 0;
    }

    /** Subclasses can override to specify where to obtain the feed item images.
	 * @return a value specifying the source of the feed item images (default Media)
	 */
	public FeedImageSource getImageSource() {
		return FeedImageSource.MediaContent;
	}

	private SimpleDateFormat _dateformat;
	
	/**
	 * @param format the date format string to use when displaying dates (default "MM/dd/yy 'at' hh:mm a ")
	 */
	public void setDateFormat(String format) {
		_dateformat = new SimpleDateFormat(format, new Locale("pt", "BR"));
	}
	
	
	private ProgressDialog _progressDialog;
	private ListView lv;

	/** Shows a progress dialog.
	 * @param title
	 * @param text
	 */
	public void showProgressDialog(String title, String text) {
		_progressDialog = ProgressDialog.show(getActivity(), title, text);
	}
	
	/** Closes the currently open progress dialog, if there is one.
	 */
	public void cancelProgressDialog() {
		if (_progressDialog != null) {
			_progressDialog.cancel();
		}
	}
	
	public void customizeItemView(Item item, View v) {

    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        
		View view = inflater.inflate(getListLayoutId(), null);
        
        setDateFormat("MM/dd/yy 'at' hh:mm a ");
        
        lv = (ListView) view.findViewById(R.id.feed_list);
        lv.setTextFilterEnabled(true);
        
		showProgressDialog("Loading...", "Downloading latest feed data");
		FeedFetcher fetcher = new FeedFetcher();
		String[] feedUrl = getFeedUrl();
		fetcher.execute(feedUrl);
    	
    	lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				Item item = (Item) adapter.getItemAtPosition(position);
				onFeedItemClick(item);
				
			}
		});
    	
    	return view;
    }
    
	protected void onFeedItemClick(Item item) {
		Uri url = Uri.parse(item.getLink());
		if (url != null) {
			Intent intent = new Intent(Intent.ACTION_VIEW, url);
			startActivity(intent);
		}
	}
	
	/** Subclasses must override this to provide their feed url.
	 */
	public abstract String[] getFeedUrl();
	
	/** Class for fetching the feed content in the background.
	 * 
	 */
	public class FeedFetcher extends AsyncTask<String, Integer, Integer> {

		Feed _feed;
		List<Feed> feeds = new ArrayList<Feed>();
		
		@Override
		protected Integer doInBackground(String... params) {

			for (int i = 0; i < params.length; i++) {
				Reader reader = new Reader(params[i]);
				feeds.add(reader.fetchFeed());
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			cancelProgressDialog();
			if (feeds.size() > 0)
				lv.setAdapter(new FeedAdapter(feeds));
			else {
				Log.w("FeedFetcher", "Unable to fetch feed. See previous errors.");
				Toast.makeText(FeedFragment.this.getActivity(), "There was an error getting the feed. Please try again later.", Toast.LENGTH_LONG).show();
			}
		}
		
	}
	

	/** A list adapter that maps a feed to a list view.
	 *
	 */
	public class FeedAdapter extends BaseAdapter {
	
		private List<Feed> feeds;

        public FeedAdapter() {

        }
		
		public FeedAdapter(List<Feed> feed) {
			feeds = feed;
		}
		
		public int getAllItemCount() {
			int count = 0;
			for (Feed feed : feeds) {
				count += feed.getItemCount();
			}
			return count;
		}
		
		public Feed getFeedAtPositionInList(int position) {
			
			for (Feed feed : feeds) {
				if(position >= feed.getItemCount()) {
					position -= feed.getItemCount();
				} else {
					return feed;
				}
			}
			return null;
		}
		
		public Item getItemAtPositionInList(int position) {
			
			for (Feed feed : feeds) {
				if(position >= feed.getItemCount()) {
					position -= feed.getItemCount();
				} else {
					return feed.getItem(position);
				}
			}
			return null;
		}
		
		
		@Override
		public int getCount() {
			if (feeds == null)
				return 0;
			if (getMaxItems() > -1 && getMaxItems() < this.getAllItemCount())
				return getMaxItems();
			return this.getAllItemCount();
		}
	
		@Override
		public Object getItem(int position) {
			if (feeds == null)
				return null;
			if (position >= this.getAllItemCount())
				return null;
			return this.getItemAtPositionInList(position);
		}
	
		@Override
		public long getItemId(int position) {
			return 0;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	
			View view = null;
			
			if (convertView == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				view = inflater.inflate(getItemLayoutId(), parent, false);
			}
			else {
				view = convertView;
			}
			
			Item item = (Item)getItem(position);
			if (item == null)
				return view;
			
			TextView titleView = (TextView)view.findViewById(R.id.feed_item_title);
			if (titleView != null && showTitleAuthorOrBoth() != 1)
				titleView.setText(item.getCleanTitle());
            else
                titleView.setVisibility(View.GONE);

            TextView authorView = (TextView)view.findViewById(R.id.feed_item_author);
            if (authorView != null && showTitleAuthorOrBoth() != 0)  {
                String txt =  item.getCleanAuthor();
                if(txt != null) authorView.setText(txt);
                else authorView.setVisibility(View.GONE);
            }
            else
                authorView.setVisibility(View.GONE);

            TextView descView = (TextView)view.findViewById(R.id.feed_item_description);
			if (descView != null)
				descView.setText(item.getCleanDescription());
			
			TextView pubDateView = (TextView)view.findViewById(R.id.feed_item_pubDate);
			if (pubDateView != null) {
				if (isDateVisible() && item.getPubDate() != null) {
					pubDateView.setVisibility(View.VISIBLE);
					pubDateView.setText(_dateformat.format(item.getPubDate()));
				}
				else
					pubDateView.setVisibility(View.GONE);
			}
			
			ImageView imageView = (ImageView)view.findViewById(R.id.feed_item_image);
			if (imageView != null) {
				FeedImageSource imageSource = getImageSource();
				if (isImageVisible() && imageSource != FeedImageSource.None) {
					imageView.setVisibility(View.VISIBLE);
					switch (imageSource) {
					case MediaContent:
						MediaContent mc = item.getMediaContent();
						imageView.setImageBitmap(null);
						if (mc != null) {
							this.getFeedAtPositionInList(position).getImageManager().download(mc.getUrl(), imageView);
						}
						break;
					case MediaThumbnail:
						MediaThumbnail mt = item.getMediaThumbnail();
						imageView.setImageBitmap(null);
						if (mt != null) {
							this.getFeedAtPositionInList(position).getImageManager().download(mt.getUrl(), imageView);
						}
						break;
					default:
						Log.w(getClass().getName(), "Don't know how to get an image from source type: " + imageSource.toString());
					}
				}
				else {
					imageView.setVisibility(View.GONE);
				}
			}
			customizeItemView(item, view);
			return view;
		}
	
	}
	
}
