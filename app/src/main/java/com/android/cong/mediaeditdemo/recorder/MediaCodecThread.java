package com.android.cong.mediaeditdemo.recorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

/**
 * 使用子线程进行屏幕录制
 * Created by xiaokecong on 09/06/2017.
 */

public class MediaCodecThread extends Thread {
    private int width;
    private int height;
    private int dpi;
    private String outFilePath;

    private MediaProjection mediaProjection;
    private MediaCodec encoder;
    private MediaMuxer muxer;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private MediaCodec.BufferInfo bufferInfo;
    private int videoTrackIndex = -1;

    private AtomicBoolean quit;
    private boolean muxerStarted;

    private final String MIME_TYPE = "video/avc";
    private final int FRAME_RATE = 30; // 帧频
    private final int IFRAME_INTERVAL = 10; // 10s between I-frames
    private final int TIMEOUT_US = 10000;
    private final int BIT_RATE = 5 * 1024 * 1024; // 5Mbps

    public MediaCodecThread(int width, int height, int dpi, MediaProjection mediaProjection, String outFilePath) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
        this.outFilePath = outFilePath;
        this.mediaProjection = mediaProjection;

        quit = new AtomicBoolean(false);
        bufferInfo = new MediaCodec.BufferInfo();
    }

    public MediaCodecThread(MediaProjection mediaProjection) {
        this(640, 480, 1, mediaProjection,
                Environment.getExternalStorageDirectory() + "/recordmaster/out_codec_recorder.mp4");
    }

    public void quit() {
        quit.set(true);
    }

    @Override
    public void run() {
        prepareEncoder();

        try {
            muxer = new MediaMuxer(outFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            virtualDisplay = mediaProjection.createVirtualDisplay("recorder", width, height, dpi, DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);

            recordVirtualDisplay();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    private void recordVirtualDisplay() {
        while (!quit.get()) {
            int index = encoder.dequeueOutputBuffer(bufferInfo,TIMEOUT_US);
            Log.d("===>xkc","dequeue output buffer :" +index);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                resetOutputFormat();
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (index >= 0) {
                if (! muxerStarted){
                    throw new IllegalStateException("MediaMuxer does not add track before");
                }
                encodeVideoTrack(index);

                encoder.releaseOutputBuffer(index,false);
            }
        }
    }

    private void encodeVideoTrack(int index) {
        ByteBuffer encodeData = encoder.getOutputBuffer(index);

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            bufferInfo.size = 0;
        }

        if (bufferInfo.size == 0) {
            encodeData = null;
        }

        if (encodeData != null) {
            encodeData.position(bufferInfo.offset);
            encodeData.limit(bufferInfo.size);
        }
    }

    private void resetOutputFormat() {
        if (muxerStarted) {
            throw new IllegalStateException("MediaMuxer format has already changed");
        }

        MediaFormat format = encoder.getOutputFormat();
        videoTrackIndex = muxer.addTrack(format);
        muxer.start();
        muxerStarted = true;

        Log.d("===>xkc","videoTrackIndex="+videoTrackIndex);

    }

    private void release() {

    }

    /**
     * 初始化MediaCodec编码器
     */
    private void prepareEncoder() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();

            encoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
