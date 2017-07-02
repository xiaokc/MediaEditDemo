package com.android.cong.mediaeditdemo.videomerge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

/**
 * Created by xiaokecong on 30/06/2017.
 */

public class VideoDecodeEncodeMuxTest {
    private MediaExtractor extractor;
    private MediaFormat inputFormat;

    private MediaCodec decoder;

    private MediaCodec encoder;

    private MediaMuxer muxer;

    private final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private int OUTPUT_VIDEO_BIT_RATE = 2000000;         // 2Mbps
    private int OUTPUT_VIDEO_FRAME_RATE = 15;            // 15fps
    private int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;       // 10 seconds between I-frames
    private int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888;
    private int TIMEOUT_USEC = 10000;

    private LinkedList<MediaCodec.BufferInfo> decodedInfoList = new LinkedList<>();

    private String outFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/video_editor/out_merged.mp4";
    private boolean isMuxing = false;
    private int trackIdx = -1;

    public void test(String path) {
        extractor = createExtractor(path);
        int videoTrack = getAndSelectVideoTrack(extractor);
        inputFormat = extractor.getTrackFormat(videoTrack);

        Log.d("===>xkc", "inputFormat:" + inputFormat);

        try {
            muxer = new MediaMuxer(outFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
            decoder.configure(inputFormat, null, null, 0);
            decoder.start();

            boolean sawEOS = false;

            while (true) {
                if (!sawEOS) {
                    int inputBufferIdx = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufferIdx >= 0) {
                        ByteBuffer buffer = decoder.getInputBuffer(inputBufferIdx);
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize >= 0) {
                            decoder.queueInputBuffer(inputBufferIdx, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        } else {
                            decoder.queueInputBuffer(inputBufferIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawEOS = true;
                        }

                    }
                }

                MediaCodec.BufferInfo decodeInfo = new MediaCodec.BufferInfo();
                int outputBufferIdx = decoder.dequeueOutputBuffer(decodeInfo, TIMEOUT_USEC);
                switch (outputBufferIdx) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.i("===>xkc", "decoder: format changed, new format is: " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.i("===>xkc", "decoder dequeue output time out");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.i("===>xkc", "decoder: dequeue output buffers changed");
                        break;
                    default:
                        Log.i("===>xkc", "decoder: dequeue normal");
                        decodedInfoList.add(decodeInfo);
                        decoder.releaseOutputBuffer(outputBufferIdx, false);
                        break;
                }
                if ((decodeInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i("===>xkc", "outputbuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();

            doEncode();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void doEncode() {
        try {
            encoder = MediaCodec.createEncoderByType(OUTPUT_VIDEO_MIME_TYPE);
            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE,
                    inputFormat.getInteger(MediaFormat.KEY_WIDTH), inputFormat.getInteger(MediaFormat.KEY_HEIGHT));
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);
            encoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            boolean sawEOS = false;

            while (true) {
                if (!sawEOS) {
                    int inputBufferIdx = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufferIdx != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (decodedInfoList.size() <= 0) {
                            encoder.queueInputBuffer(inputBufferIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawEOS = true;
                        } else {
                            MediaCodec.BufferInfo info = decodedInfoList.poll();
                            encoder.queueInputBuffer(inputBufferIdx, 0, info.size, info.presentationTimeUs, info.flags);
                            Log.i("===>xkc", "to encode info,size=" + info.size + ",presentationTime=" + info
                                    .presentationTimeUs + ",flags=" + info.flags);
                        }

                    }
                }
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

            //            encoder.release();
            //            encoder.stop();
            //            muxer.release();
            //            muxer.stop();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
