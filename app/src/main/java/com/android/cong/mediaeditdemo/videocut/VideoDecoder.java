package com.android.cong.mediaeditdemo.videocut;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

/**
 * Created by xiaokecong on 07/06/2017.
 */

public class VideoDecoder {
    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;
    private MediaFormat mediaFormat;
    private String mime;

    private int sourceVTrack = 0;
    private int videoSourceInputSize = 0;
    private long videoDuration = 0;
    private int videoTrackIndex = -1;

    private int sourceATrack = 0;
    private int audioSourceInputSize = 0;
    private long audioDuration = 0;
    private int audioTrackIndex = -1;

    public boolean decodeVideo(String url, long clipPoint, long clipDuration) {
        // 创建多媒体分离器
        mediaExtractor = new MediaExtractor();

        try {
            // 设置多媒体数据源
            mediaExtractor.setDataSource(url);

            // 创建合成器
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("===>xkc", "error path:" + e.getMessage());
        }

        // 获取每个轨道信息
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);

                mime = mediaFormat.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith("video/")) {
                    sourceVTrack = i;

                    int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoSourceInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);

                    // 剪切点超过了视频最大长度
                    if (clipPoint >= videoDuration) {
                        Log.e("===>xkc", "clip point error");
                        return false;
                    }

                    if ((clipDuration != 0) && (clipPoint + clipDuration) >= videoDuration) {
                        Log.e("===>xkc", "clip duration error");
                        return false;
                    }

                    Log.d("===>xkc",
                            "width=" + width + ",height=" + height + ",videoSourceInputSize=" + videoSourceInputSize
                                    + ","
                                    + "videoDuration=" + videoDuration);

                    // 向合成器中添加视频轨
                    videoTrackIndex = mediaMuxer.addTrack(mediaFormat);

                } else if (mime.startsWith("audio/")) {
                    sourceATrack = i;

                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioSourceInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);

                    Log.d("===>xkc", "sampleRate=" + sampleRate + ",channelCount=" + channelCount
                            + "audioSourceInputSize=" + audioSourceInputSize + ",audioDuration=" + audioDuration);

                    // 向合成器中添加音频轨
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }

                Log.d("===>xkc", "file mime=" + mime);
            } catch (Exception e) {
                Log.e("===>xkc", "read error");
            }

        }
        // start()方法一定要在addTrack之后
        mediaMuxer.start();

        // 分配缓冲
        ByteBuffer inputBuffer = ByteBuffer.allocate(videoSourceInputSize);

        // 1. 先进行视频数据处理
        mediaExtractor.selectTrack(sourceVTrack);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        videoInfo.presentationTimeUs = 0;

        // 计算采样（帧）率
        long videoSampleTime = 0;

        // 获取源视频相邻帧之间的时间间隔
        mediaExtractor.readSampleData(inputBuffer, 0);

        // 跳过第一个I帧
        if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
            mediaExtractor.advance();
        }

        mediaExtractor.readSampleData(inputBuffer, 0);
        long firstVideoPTS = mediaExtractor.getSampleTime();

        mediaExtractor.advance();
        mediaExtractor.readSampleData(inputBuffer, 0);
        long secondPTS = mediaExtractor.getSampleTime();
        videoSampleTime = Math.abs(secondPTS - firstVideoPTS);

        Log.d("===>xkc", "videSampleTime is:" + videoSampleTime);

        // 选择起点
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                // 这里一定要释放选择的轨道，不然另一个轨道就无法选中了
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }

            int trackIndex = mediaExtractor.getSampleTrackIndex();

            // 获取时间戳
            long presentationTimeUs = mediaExtractor.getSampleTime();
            // 获取帧类型，只能识别是否是关键帧
            int sampleFlag = mediaExtractor.getSampleFlags();
            Log.d("===>xkc", "trackIndex=" + trackIndex + ",presentationTimeUs=" + presentationTimeUs + ","
                    + "sampleFlag=" + sampleFlag + ",sampleSize=" + sampleSize);

            // 剪辑时间到了，跳出
            if ((clipDuration != 0) && ((clipPoint + clipDuration) < presentationTimeUs)) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }

            mediaExtractor.advance();
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            videoInfo.flags = sampleFlag;
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            videoInfo.presentationTimeUs += presentationTimeUs;

        }

        // 2. 音频处理
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        long audioSampleTime;

        // 获取音频帧时长
        mediaExtractor.readSampleData(inputBuffer, 0);

        // 跳过第一个sample
        if (mediaExtractor.getSampleTime() == 0) {
            mediaExtractor.advance();
        }

        mediaExtractor.readSampleData(inputBuffer, 0);
        long firstAudioPTS = mediaExtractor.getSampleTime();

        mediaExtractor.advance();
        mediaExtractor.readSampleData(inputBuffer, 0);
        long secondAudioPTS = mediaExtractor.getSampleTime();

        audioSampleTime = Math.abs(secondAudioPTS - firstAudioPTS);
        Log.d("===>xkc", "audioSampleTime is:" + audioSampleTime);

        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }

            int trackIndex = mediaExtractor.getSampleTrackIndex();

            long presentationTimeUs = mediaExtractor.getSampleTime();

            Log.d("===>xkc", "trackIndex=" + trackIndex + ",presentationTimeUs=" + presentationTimeUs + ","
                    + ",sampleSize=" + sampleSize);

            // 剪辑时间到了，跳出
            if ((clipDuration != 0) && ((clipPoint + clipDuration) < presentationTimeUs)) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }

            mediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs += presentationTimeUs;

        }

        // 全部写完后，释放MediaExtractor和MediaMuxer
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;

        return true;
    }
}

