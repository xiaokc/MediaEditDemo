package com.android.cong.mediaeditdemo.audiomixer;

import java.io.IOException;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/**
 * Created by xiaokecong on 19/06/2017.
 */

public class MixUtil {
    /**
     * 将两个mp3文件解码为pcm数据
     * @param firstAudioPath
     * @param secondAudioPath
     * @param callback 合成回调
     */
    public static void muxTwoAudios(String firstAudioPath, String secondAudioPath, MuxCallback callback) {
    }

    /**
     * 解码mp3 音频文件，返回相应格式数据
     * @param audioPath
     * @return 音频时长，采样率，通道数
     */
    private static Object[] decodeAudio(String audioPath) {
        Object[] res = new Object[3];
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(audioPath);
            for (int i = 0; i < extractor.getTrackCount(); i ++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
