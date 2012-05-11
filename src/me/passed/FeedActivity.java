package me.passed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	private Map<Integer, Bitmap> imgCache;

	private Cursor imgCursor;

	private Handler imgHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long start = System.nanoTime();
		
		ContentResolver contentResolver = getContentResolver();
		String[] projection = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
		imgCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " desc");
		
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
		
		Log.i("perf-init-cache", "" + (System.nanoTime() - start));
		
		imgCache = new LinkedHashMap<Integer, Bitmap>() {
			private static final long serialVersionUID = -7863147206244901890L;

			@Override
			protected boolean removeEldestEntry(Entry<Integer, Bitmap> eldest) {
				return size() > 50;
			}

		};

		imgHandler = new Handler();
		setContentView(R.layout.feed);
		GridView g = (GridView) findViewById(R.id.feedGrid);
		g.setColumnWidth(96);
		g.setNumColumns(GridView.AUTO_FIT);
		g.setVerticalSpacing(5);
		g.setAdapter(new ImageAdapter(this));

		Log.i("perf-init-all", "" + (System.nanoTime() - start));
	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
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
			// int newWidth = 96;
			// int newHeight = 96;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				// imageView.setLayoutParams(new GridView.LayoutParams(newWidth, newHeight));
				// imageView.setAdjustViewBounds(false);
				// imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				// imageView.setPadding(0, 0, 0, 0);
			} else {
				imageView = (ImageView) convertView;
			}

			final int imgId = imgIdList.get(position);
			Bitmap cache = imgCache.get(imgId);
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
						imgCache.put(imgId, bm);
					}

				});
			}

			return imageView;
		}

	}

}