package com.android.cong.mediaeditdemo.recorder;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

/**
 * Created by xiaokecong on 06/06/2017.
 */

public class MediaCodecService extends Service {
    private boolean running;
    private boolean muxerStarted;
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private MediaCodec.BufferInfo bufferInfo;
    private int videoTrackIndex = -1;

    private int width;
    private int height;
    private int dpi;

    private final String MIME_TYPE = "video/avc";
    private final int FRAME_RATE = 30; // 帧频
    private final int IFRAME_INTERVAL = 10; // 10s between I-frames
    private final int TIMEOUT_US = 10000;

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("MediaCodecService", Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
    }

    public boolean startRecord() {
        if (null == mediaProjection || running) {
            return false;
        }

        prepareEncoder();
        prepareMediaMuxer();
        prepareVirtualDisplay();

        while (!running) {
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 后续输出格式变化
                resetOutputFormat();
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 请求超时
                Log.e("===>xkc", "retrieving buffers time out");

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (index >= 0) {
                if (!muxerStarted) {
                    throw new IllegalStateException("MediaMuxer does not call addTrack(format)");
                }
                encodeToVideoTrack(index);

                mediaCodec.releaseOutputBuffer(index, false);
            }
        }

        running = true;
        return true;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }

        running = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaMuxer.stop();
        mediaMuxer.release();
        virtualDisplay.release();
        return true;
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once
        if (muxerStarted) {
            throw new IllegalStateException("output format already changed");
        }

        MediaFormat format = mediaCodec.getOutputFormat();

        Log.i("===>xkc", "ouput format changed. new format:" + format);
        videoTrackIndex = mediaMuxer.addTrack(format);
        mediaMuxer.start();
        muxerStarted = true;
        Log.i("===>xkc", "started media muxer, video track index:" + videoTrackIndex);

    }


    private void encodeToVideoTrack(int index) {
        ByteBuffer encodeData = mediaCodec.getOutputBuffer(index);

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got the INFO_OUTPUT_FORMAT_CHANGED
            // status, ignore it.
            Log.i("===>xkc","ignore BUFFER_FLAG_CODEC_CONFIG");
            bufferInfo.size = 0;
        }

        if (bufferInfo.size == 0) {
            Log.i("===>xkc","info.size == 0, drop it");
            encodeData = null;
        } else {
            Log.i("===>xkc","got buffer, info:size="+bufferInfo.size+",presentationTimeUS="+bufferInfo
                    .presentationTimeUs+",offset="+bufferInfo.offset);
        }

        if (encodeData != null) {
            encodeData.position(bufferInfo.offset);
            encodeData.limit(bufferInfo.offset + bufferInfo.size);
            mediaMuxer.writeSampleData(videoTrackIndex, encodeData, bufferInfo);
            Log.i("===>xkc","sent " +bufferInfo.size+" bytes to muxer");
        }

    }

    private void prepareEncoder() {
        bufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5 * 1024 * 1024);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        Log.i("===>xkc", "create video format:" + format);

        try {
            mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            Log.i("===>xkc", "create input surface:" + surface);

            mediaCodec.start();

        } catch (IOException e) {
            Log.e("===>xkc","prepareEncoder error:"+e.getMessage());
            e.printStackTrace();
        }

    }

    private void prepareMediaMuxer() {
        try {
            mediaMuxer = new MediaMuxer(System.currentTimeMillis() + "_codec.mp4", MediaMuxer.OutputFormat
                    .MUXER_OUTPUT_MPEG_4);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void prepareVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MediaCodecService", width, height, dpi, DisplayManager
                .VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = 720;
        this.height = 480;
        this.dpi = 1;

    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MediaCodecBinder();
    }

    public class MediaCodecBinder extends Binder {
        public MediaCodecService getMediaCodecService() {
            return MediaCodecService.this;
        }
    }
}
