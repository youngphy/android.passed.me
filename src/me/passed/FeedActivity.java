package me.passed;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class FeedActivity extends Activity {
	private static final String TAG = FeedActivity.class.getSimpleName();

	private List<Integer> list;

	private Cursor cursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ContentResolver contentResolver = getContentResolver();
		String[] projection = new String[] { MediaStore.Images.Thumbnails.IMAGE_ID };
		cursor = contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Thumbnails.DEFAULT_SORT_ORDER);
		cursor.moveToFirst();
		list = new ArrayList<Integer>(cursor.getCount());
		while (true) {
			list.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID)));
			if (!cursor.moveToNext()) {
				break;
			}
		}
		cursor.close();

		setContentView(R.layout.feed);

		GridView g = (GridView) findViewById(R.id.feedGrid);
		g.setAdapter(new ImageAdapter(this));
	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return list.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			int newWidth = 96;
			int newHeight = 96;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(newWidth, newHeight));
				imageView.setAdjustViewBounds(false);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(0, 0, 0, 0);
			} else {
				imageView = (ImageView) convertView;
			}
			int imgId = list.get(position);
			Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), imgId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
			if (bm != null) {
				int width = bm.getWidth();
				int height = bm.getHeight();
				Log.d(TAG, String.format("pid:%s pos:%s w:%s h:%s", imgId, position, width, height));
				imageView.setImageBitmap(bm);
			} else {
				Log.e(TAG, String.format("pid:%s pos:%s", imgId, position));
			}
			return imageView;
		}

	}

}