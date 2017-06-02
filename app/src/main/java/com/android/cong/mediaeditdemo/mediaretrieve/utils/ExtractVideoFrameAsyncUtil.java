package com.android.cong.mediaeditdemo.mediaretrieve.utils;

import com.android.cong.mediaeditdemo.mediaretrieve.entity.ImageItem;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class ExtractVideoFrameAsyncUtil {
    private int extractW;
    private int extractH;
    private Handler handler;

    private boolean stop;

    public ExtractVideoFrameAsyncUtil(int extractW, int extractH, Handler handler) {

        this.extractH = extractH;
        this.extractW = extractW;
        this.handler = handler;
    }

    public void getVideoFrame(String videoPath, String outputFileDirPath, long startPosition, long endPosition,
                              int thumbnailCount) {

        MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(videoPath);

        long interval = (endPosition - startPosition) / (thumbnailCount - 1);

        for (int i = 0; i < thumbnailCount; i++) {
            if (stop) {
                mRetriever.release();
                break;
            }

            long time = startPosition + interval * i;

            if (i == thumbnailCount - 1) {
                if (interval > 1000) {
                    String path = extractFrame(mRetriever, endPosition - 800, outputFileDirPath);
                    sendAPic(path, endPosition - 800);
                } else {
                    String path = extractFrame(mRetriever, endPosition, outputFileDirPath);
                    sendAPic(path, endPosition);
                }
            } else {
                String path = extractFrame(mRetriever, time, outputFileDirPath);
                sendAPic(path, time);
            }

        }
        mRetriever.release();

    }

    private String extractFrame(MediaMetadataRetriever retriever, long time, String outputFileDirPath) {
        Bitmap bitmap = retriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (bitmap != null) {
            Bitmap bitmapNew = scaleImage(bitmap);
            String path = PictureUtil.saveImageToSD(bitmapNew, outputFileDirPath, System.currentTimeMillis()
                    + "_" + time + PictureUtil.POSTFIX);
            if (bitmapNew != null && !bitmapNew.isRecycled()) {
                bitmapNew.recycle();
                bitmapNew = null;
            }
            return path;
        }

        return null;

    }

    /**
     * 设置固定宽度，高度随之变化，使图片不会变形
     *
     * @param bm
     *
     * @return
     */
    private Bitmap scaleImage(Bitmap bm) {
        if (null == bm) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = extractW * 1.0f / width;
        float scaleHeight = extractH * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);

        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }
        return bitmap;

    }

    /**
     * 提取成功一张就add一张
     * @param path
     * @param time
     */
    private void sendAPic(String path, long time) {

        ImageItem item = new ImageItem();
        item.path = path;
        item.time = time;
        Message msg = handler.obtainMessage(0);
        msg.obj = item;
        handler.sendMessage(msg);
    }

}
