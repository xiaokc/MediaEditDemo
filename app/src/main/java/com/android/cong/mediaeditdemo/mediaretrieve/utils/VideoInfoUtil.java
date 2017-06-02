package com.android.cong.mediaeditdemo.mediaretrieve.utils;

import java.io.File;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class VideoInfoUtil {

    private MediaMetadataRetriever mRetriever;
    private long videoLen = 0; // 毫秒

    public VideoInfoUtil(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            throw new RuntimeException("视频路径不能为空");
        }

        File file = new File(videoPath);
        if (!file.exists()) {
            throw new RuntimeException("视频不存在");
        }

        mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(file.getAbsolutePath());

        String len = getVideoLength();
        videoLen = TextUtils.isEmpty(len) ? 0 : Long.valueOf(len);

    }

    /**
     * 获取视频时长，即长度
     * @return
     */
    public String getVideoLength() {
        return mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    }

    /**
     * 获取视频宽度
     * @return
     */
    public long getVideoWidth() {
        String w = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        int width = -1;
        if (!TextUtils.isEmpty(w)) {
            width = Integer.valueOf(w);
        }
        return width;
    }

    /**
     * 获取视频高度
     * @return
     */
    public long getVideoHeight() {
        String w = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        int height = -1;
        if (!TextUtils.isEmpty(w)) {
            height = Integer.valueOf(w);
        }
        return height;
    }

    /**
     * 获取视频旋转角度
     * @return
     */
    public long getVideoRotateDegree() {
        String d = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        int degree = 0;
        if (! TextUtils.isEmpty(d)) {
            degree = Integer.valueOf(d);
        }
        return degree;
    }

    /**
     * 获取视频类型
     * @return
     */
    public String getVideoMimeType() {
         return mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
    }

    /**
     * 获取视频最有代表性的，即关键帧
     * @return
     */
    public Bitmap getResponsiveFrame() {
        return mRetriever.getFrameAtTime();
    }

    /**
     * 获取某个时刻的关键帧
     * @param timeMs
     * @return
     */
    public Bitmap getFrameAtTime(long timeMs) {
        Bitmap bitmap = null;

        for (long i = timeMs; i < videoLen; i += 1000) {
            bitmap = mRetriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (bitmap != null) {
                break;
            }
        }
        return bitmap;
    }


    public void destroy() {
        if (mRetriever != null) {
            mRetriever.release();
        }
    }
}
