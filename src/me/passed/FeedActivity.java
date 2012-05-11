package me.passed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class FeedActivity extends Activity {

	private List<Integer> imgIdList;

	private DisplayMetrics metrics;

	private int scrollBarWidth = 0; // vertical scrollbar disabled

	private int columnWidth = 96;

	private int gridSpacing = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long start = System.nanoTime();

		ContentResolver contentResolver = getContentResolver();
		String[] projection = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED };
		Cursor imgCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

		Log.i("perf-init-query", "" + (System.nanoTime() - start));

		if (imgCursor != null) {
			try {
				imgCursor.moveToFirst();
				imgIdList = new ArrayList<Integer>();
				while (true) {
					int imgId = imgCursor.getInt(imgCursor.getColumnIndex(MediaStore.Images.Media._ID));
					String imgPath = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.Media.DATA));
					if (imgPath.startsWith("/sdcard/DCIM/Camera/")) {
						imgIdList.add(imgId);
					}
					if (!imgCursor.moveToNext()) {
						break;
					}
				}
			} finally {
				imgCursor.close();
			}
		}

		Collections.sort(imgIdList, new Comparator<Integer>() {

			@Override
			public int compare(Integer arg0, Integer arg1) {
				return arg1 - arg0;
			}

		});

		Log.i("perf-init-idcache", "" + (System.nanoTime() - start));

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		setContentView(R.layout.feed);

		GridView g = (GridView) findViewById(R.id.feedGrid);
		g.setColumnWidth(columnWidth);
		int numColumns = (metrics.widthPixels - scrollBarWidth + gridSpacing) / (columnWidth + gridSpacing);
		g.setNumColumns(numColumns);
		g.setVerticalSpacing(gridSpacing);
		g.setHorizontalSpacing(gridSpacing);
		int leftright = metrics.widthPixels - numColumns * columnWidth - (numColumns - 1) * gridSpacing;
		int left = leftright / 2;
		int right = leftright - left - scrollBarWidth;
		g.setPadding(left, 0, right, 0);
		g.setVerticalScrollBarEnabled(false);
		g.setHorizontalScrollBarEnabled(false);

		g.setAdapter(new ImageAdapter(this));

		Log.i("perf-init-all", "" + (System.nanoTime() - start));
	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		private Map<Integer, Bitmap> imgCache;

		private int cacheSize = 100;

		private Handler imgHandler;

		private ReadWriteLock rwLock = new ReentrantReadWriteLock();

		private Lock rLock = rwLock.readLock();

		private Lock wLock = rwLock.writeLock();

		public ImageAdapter(Context c) {
			mContext = c;
			imgHandler = new Handler();
			imgCache = new LinkedHashMap<Integer, Bitmap>() {
				private static final long serialVersionUID = -7863147206244901890L;

				@Override
				protected boolean removeEldestEntry(Entry<Integer, Bitmap> eldest) {
					return size() > cacheSize;
				}

			};
		}

		public int getCount() {
			return imgIdList.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			final ImageView imageView;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(columnWidth, columnWidth));
				imageView.setAdjustViewBounds(false);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(0, 0, 0, 0);
			} else {
				imageView = (ImageView) convertView;
			}

			final int imgId = imgIdList.get(position);

			Bitmap cache = null;
			try {
				rLock.lock();
				cache = imgCache.get(imgId);
			} finally {
				rLock.unlock();
			}
			if (cache != null) {
				// Log.d("cacheImg", String.format("pid:%s pos:%s cachesize:%s", imgId, position, imgCache.size()));
				imageView.setImageBitmap(cache);
			} else {
				imgHandler.post(new Runnable() {

					@Override
					public void run() {
						Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), imgId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
						// Log.d("newImg", String.format("pid:%s pos:%s w:%s h:%s", imgId, position, bm.getWidth(), bm.getHeight()));
						imageView.setImageBitmap(bm);
						try {
							wLock.lock();
							imgCache.put(imgId, bm);
						} finally {
							wLock.unlock();
						}
					}

				});
			}

			return imageView;
		}
	}

}