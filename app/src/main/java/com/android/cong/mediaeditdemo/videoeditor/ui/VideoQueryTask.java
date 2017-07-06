package com.android.cong.mediaeditdemo.videoeditor.ui;

import java.util.ArrayList;
import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoItem;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by xiaokecong on 06/07/2017.
 */

public class VideoQueryTask extends AsyncTask<Void, Integer, List<VideoItem>> {
    private Context mContext;
    private OnVideoQueryDoneListener mListener;

    public VideoQueryTask(Context context) {
        this.mContext = context;
    }

    public interface OnVideoQueryDoneListener {
        void onDone(List<VideoItem> videoItems);
    }

    @Override
    protected List<VideoItem> doInBackground(Void... params) {
        Log.i("===>xkc","doInBackground...");
        List<VideoItem> list = new ArrayList<>();
        final String[] mProjection = new String[] {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION

        };
        Cursor cursor =
                mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mProjection, null,
                        null, MediaStore.Video.Media.DATE_MODIFIED + " desc");
        if (null == cursor || cursor.getColumnCount() <= 0) {
            return null;
        }
        cursor.moveToFirst();
        String name;
        String path;
        Bitmap thumb;

        long size;
        int width;
        int height;
        long lastModifiedTime;
        long duration;

        while (cursor.moveToNext()) {
            VideoItem item = new VideoItem();
            name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
            lastModifiedTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
            width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH));
            height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT));
            duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));

//            thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
//            if (null == thumb) {
//                thumb = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.format_media);
//            }
            item.setName(name);
            item.setPath(path);
//            item.setThumbBmp(thumb);
            item.setSize(size);
            item.setLastModifiedTime(lastModifiedTime);
            item.setWidth(width);
            item.setHeight(height);
            item.setDuration(duration);

            list.add(item);

        }
        if (cursor != null) {
            cursor.close();
        }

        return list;
    }

    @Override
    protected void onPostExecute(List<VideoItem> videoItems) {
        super.onPostExecute(videoItems);
        if (mListener != null) {
            mListener.onDone(videoItems);
            Log.i("===>xkc","查询完毕");
        }
    }

    public void setOnVideoQueryDoneListener(OnVideoQueryDoneListener listener) {
        this.mListener = listener;
    }
}
