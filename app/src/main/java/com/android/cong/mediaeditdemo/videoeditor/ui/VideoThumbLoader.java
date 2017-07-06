package com.android.cong.mediaeditdemo.videoeditor.ui;

import com.android.cong.mediaeditdemo.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * Created by xiaokecong on 06/07/2017.
 */

public class VideoThumbLoader {
    private Context mContext;

    private LruCache<String, Bitmap> lruCache; // 使用LruCache 缓存策略

    public VideoThumbLoader(Context context) {
        this.mContext = context;

        int maxMemory = (int) Runtime.getRuntime().maxMemory(); // 获取最大运行内存
        int maxSize = maxMemory / 4; // 缓存大小为最大运行内存的1/4

        lruCache = new LruCache<String, Bitmap>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount(); // 每次存入缓存的时候会调用
            }
        };

    }

    private void addVideoThumbToCache(String path, Bitmap bitmap) {
        if (null == getVideoThumbFromCache(path)) {
            lruCache.put(path, bitmap);
        }
    }

    private Bitmap getVideoThumbFromCache(String path) {
        return lruCache.get(path);
    }

    public void loadVideoThumb(String videoPath, ImageView imageView) {
        Log.i("===>xkc", "loadVideoThumb...videoPath:" + videoPath);
//        if (getVideoThumbFromCache(videoPath) != null) {
//            imageView.setImageBitmap(getVideoThumbFromCache(videoPath));
//        } else {
//            new VideoThumbLoadTask(imageView, videoPath).execute();
//        }

        new VideoThumbLoadTask(imageView, videoPath).execute();

    }

    class VideoThumbLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView imageView;
        private String path;

        VideoThumbLoadTask(ImageView imageView, String videoPath) {
            this.imageView = imageView;
            this.path = videoPath;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.format_media);
            }
            if (null == getVideoThumbFromCache(path)) {
                addVideoThumbToCache(path, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (imageView.getTag().equals(path)) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
