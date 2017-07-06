package com.android.cong.mediaeditdemo.videomux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

/**
 * 使用同步方法，解码->OutputSurface->InputSurface->编码
 * Created by xiaokecong on 04/07/2017.
 */

public class VideoConcatSync {
    private int fileIndex = 0;
    private List<String> fileList;
    private String outputPath;
    private MediaMuxer muxer;
    private MediaExtractor extractor;
    private MediaCodec decoder;
    private MediaCodec encoder;

    private MediaFormat inputFormat;

    private final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private int OUTPUT_VIDEO_BIT_RATE = 2000000;         // 2Mbps
    private int OUTPUT_VIDEO_FRAME_RATE = 15;            // 15fps
    private int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;       // 10 seconds between I-frames
    private int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888;
    private int TIMEOUT_USEC = 10000;

    private InputSurface inputSurface;
    private OutputSurface outputSurface;

    private boolean encodeDone;
    private boolean isMuxing;
    private int trackIdx = -1;
    private boolean encoderStarted;

    private AtomicReference<Surface> inputSurfaceReference;

    public VideoConcatSync(List<String> fileList, String outputPath) {
        this.fileList = fileList;
        this.outputPath = outputPath;
    }

    public void start(boolean sync) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    concatSync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        if (sync) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void concatSync() {
        try {
            muxer = createMuxer();

            while (fileIndex < fileList.size()) {
                extractor = createExtractor(fileList.get(fileIndex));
                int videoTrack = getAndSelectVideoTrack(extractor);
                inputFormat = extractor.getTrackFormat(videoTrack);
                int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);

                if (!encoderStarted) {
                    initEncoder();
                }

                inputSurface.makeCurrent();
                outputSurface.setup(width, height, 0);

                decoder = createDecoder(outputSurface.getSurface());



            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initEncoder() throws IOException {
        inputSurfaceReference = new AtomicReference<>();
        encoder = MediaCodec.createEncoderByType(OUTPUT_VIDEO_MIME_TYPE);
        MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE,
                inputFormat.getInteger(MediaFormat.KEY_WIDTH), inputFormat.getInteger(MediaFormat.KEY_HEIGHT));
        outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
        outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
        outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
        outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);
        encoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurfaceReference.set(encoder.createInputSurface());
        encoder.start();
        encoderStarted = true;
        inputSurface = new InputSurface(inputSurfaceReference.get());
    }

    private MediaCodec createDecoder(Surface surface) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(OUTPUT_VIDEO_MIME_TYPE);
        decoder.configure(inputFormat, surface, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        decoder.start();
        return decoder;
    }

    private void startEncode() throws IOException {
        while (!encodeDone) {
            MediaCodec.BufferInfo encoderOutputInfo = new MediaCodec.BufferInfo();

            int outputIdx = encoder.dequeueOutputBuffer(encoderOutputInfo, TIMEOUT_USEC);

            switch (outputIdx) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.i("===>xkc",
                            "encoder output format changed, new format is:" + encoder.getOutputFormat());
                    if (!isMuxing) {
                        trackIdx = muxer.addTrack(encoder.getOutputFormat());
                        muxer.start();
                        isMuxing = true;

                    } else {
                        Log.e("===>xkc", "encoder output format changed again?");
                    }
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.i("===>xkc", "encoder: dequeue output time out");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.i("===>xkc", "encoder: dequeue output buffers changed");
                    break;
                default:
                    Log.i("===>xkc", "encoder: dequeue normal");
                    if (isMuxing) {
                        if (trackIdx != -1 && encoder.getOutputBuffer(outputIdx) != null) {
                            muxer.writeSampleData(trackIdx, encoder.getOutputBuffer(outputIdx), encoderOutputInfo);
                        } else {
                            Log.e("===>xkc", "muxer 出错");
                        }
                    }
                    encoder.releaseOutputBuffer(outputIdx, false);
                    break;
            }

            if ((encoderOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e("===>xkc", "编码结束");
                break;
            }

        }
    }

    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private MediaExtractor createExtractor(String videoSrc) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(videoSrc);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("===>xkc", "extractor setDataSouce error:" + e.getMessage());
        }
        return extractor;
    }

    private int getAndSelectVideoTrack(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); index++) {
            MediaFormat format = extractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.d("===>xkc", "video track mime:" + mime);
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }
}
