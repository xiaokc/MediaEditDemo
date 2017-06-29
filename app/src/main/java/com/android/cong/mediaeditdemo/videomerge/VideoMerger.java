package com.android.cong.mediaeditdemo.videomerge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.List;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Trace;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by xiaokecong on 07/06/2017.
 */

public class VideoMerger {
    private final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";

    private int OUTPUT_VIDEO_BIT_RATE = 2000000;         // 2Mbps
    private int OUTPUT_VIDEO_FRAME_RATE = 15;            // 15fps
    private int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;       // 10 seconds between I-frames
    private int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    private int TIMEOUT_USEC = 10000;

    private MediaExtractor videoExtractor;

    private int width;
    private int height;

    private MediaCodec videoEncoder;
    private MediaCodec videoDecoder;

    private boolean sawInputEOS;
    private boolean toNextFile;
    private boolean outputDone;

    private int fileIndex = 0;

    private String outFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/recordmaster/out_merged.mp4";

    private ByteBuffer[] videoDecoderOutputBuffers;

    private ByteBuffer[] videoOutputBuffers;

    public void mergeVideo(List<String> filePathList) throws IOException {
        long lastSamplePresentationTime = 0;
        FileOutputStream fos = new FileOutputStream(new File(outFilePath));

        while (fileIndex < filePathList.size()) {

            videoExtractor = createExtractor(filePathList.get(fileIndex));
            int videoInputTrack = getAndSelectVideoInputTrackIndex(videoExtractor);

            MediaFormat inputFormat = videoExtractor.getTrackFormat(videoInputTrack);

            width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);

            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, width, height);
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

            videoDecoder = createVideoDecoder(inputFormat);

            while (!outputDone) {
                if (toNextFile) { // 拼接下一个文件
                    break;
                }
                if (!sawInputEOS) {
                    int inputBufferId = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufferId >= 0) {
                        ByteBuffer inputBuffer = videoDecoder.getInputBuffer(inputBufferId);
                        int sampleSize = videoExtractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            videoDecoder.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawInputEOS = true;
                        } else {
                            videoDecoder.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    sampleSize,
                                    lastSamplePresentationTime + videoExtractor.getSampleTime(),
                                    0);
                            if (lastSamplePresentationTime == 0) {
                                lastSamplePresentationTime = videoExtractor.getSampleTime();
                            } else {
                                lastSamplePresentationTime += videoExtractor.getSampleTime();
                            }
                            toNextFile = !videoExtractor.advance();

                        }
                    }
                }

                videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                MediaCodec.BufferInfo videoDecoderBufferInfo = new MediaCodec.BufferInfo();
                int decoderOutputBufferId = videoDecoder.dequeueOutputBuffer(videoDecoderBufferInfo, TIMEOUT_USEC);

                if (decoderOutputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d("===>xkc", "没有decoder的buffer了");
                    continue;
                } else if (decoderOutputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i("===>xkc",
                            "videoEncoder output format changed, new format is:" + videoDecoder.getOutputFormat());
                } else if (decoderOutputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                } else if (decoderOutputBufferId < 0) {
                    Log.d("===>xkc","decode error");
                } else {
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferId, false);
                }

                videoEncoder = createVideoEncoder(outputVideoFormat);

                doEncode(videoEncoder, videoDecoderBufferInfo, lastSamplePresentationTime, fos);
                outputDone = (videoDecoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

            }

            fileIndex++;
        }

        //        videoDecoder.release();
        //        videoEncoder.release();
        //        muxer.release();
        //        videoDecoder = null;
        //        videoEncoder = null;
        //        muxer = null;

    }

    private void doEncode(MediaCodec videoEncoder, MediaCodec.BufferInfo decoderBufferInfo, long presentationTimeUS,
                          FileOutputStream fos) {
        int inputBufferId = videoEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferId >= 0) {
            videoEncoder.queueInputBuffer(inputBufferId, 0, decoderBufferInfo.size, presentationTimeUS, 0);
        }

        MediaCodec.BufferInfo videoEncoderBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferId = videoEncoder.dequeueOutputBuffer(videoEncoderBufferInfo, TIMEOUT_USEC);

        Log.i("===>xkc","the output format is:"+videoEncoder.getOutputFormat());
        while (outputBufferId >= 0) {
            ByteBuffer encoderOutputBuffer = videoOutputBuffers[outputBufferId];
            byte[] outData = new byte[videoEncoderBufferInfo.size];
            encoderOutputBuffer.get(outData);

            if (videoEncoderBufferInfo.size != 0) {
                try {
                    fos.write(outData, 0, outData.length);
                    videoEncoder.releaseOutputBuffer(outputBufferId, false);
                    outputBufferId = videoEncoder.dequeueOutputBuffer(videoEncoderBufferInfo, TIMEOUT_USEC);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private MediaCodec createVideoEncoder(MediaFormat outputVideoFormat)
            throws IOException {
        MediaCodec encoder = MediaCodec.createEncoderByType(OUTPUT_VIDEO_MIME_TYPE);
        encoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }

    private MediaCodec createVideoDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(OUTPUT_VIDEO_MIME_TYPE);
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }

    private int getAndSelectVideoInputTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            Log.d("===>xkc", "format for track " + index + " is "
                    + getMimeTypeFor(extractor.getTrackFormat(index)));
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private MediaExtractor createExtractor(String dataSource) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(dataSource);
        return extractor;
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

}
