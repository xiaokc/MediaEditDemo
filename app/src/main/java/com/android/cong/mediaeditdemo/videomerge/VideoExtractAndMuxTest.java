package com.android.cong.mediaeditdemo.videomerge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

/**
 * Created by xiaokecong on 03/07/2017.
 */

public class VideoExtractAndMuxTest {
    private static final String TAG = VideoExtractAndMuxTest.class.getSimpleName();
    private MediaExtractor extractor;
    private MediaMuxer muxer;
    private int videoTrack = -1;
    private boolean isMuxing;
    private boolean extractDone;
    private int sourceInputSize;

    private int index = -1;
    long lastSamplePTS = 0;

    public void videoExtractAndMux(List<String> filePathList, String outputFilePath) {
        if (null == filePathList || filePathList.size() <= 0) {
            Log.e(TAG, "源文件list为空");
            return;
        }

        try {
            muxer = new MediaMuxer(outputFilePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            for (int i = 0; i < filePathList.size(); i++) {
                extractor = new MediaExtractor();
                String filePath = filePathList.get(i);
                extractor.setDataSource(filePath);

                // extractor 选择视轨，muxer 添加视轨
                for (int j = 0; j < extractor.getTrackCount(); j ++) {
                    MediaFormat format = extractor.getTrackFormat(j);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        sourceInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                        index = j;
                        if (!isMuxing) {
                            videoTrack = muxer.addTrack(format);
                            muxer.start();
                            isMuxing = true;
                        }
                        break;
                    }
                }
                ByteBuffer buffer = ByteBuffer.allocate(sourceInputSize);
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                extractor.selectTrack(index);
                extractDone = false;
                while(!extractDone) {
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        extractor.unselectTrack(index);
                        break;
                    }

                    long presentationTimeUS = extractor.getSampleTime();
                    if (lastSamplePTS == 0) {
                        lastSamplePTS = presentationTimeUS;
                    }

                    if (presentationTimeUS < lastSamplePTS) {
                        presentationTimeUS += lastSamplePTS;
                    }

                    lastSamplePTS = presentationTimeUS;

                    info.flags = extractor.getSampleFlags();
                    info.size = sampleSize;
                    info.presentationTimeUs = presentationTimeUS;

                    muxer.writeSampleData(videoTrack, buffer, info);
                    extractDone = ! extractor.advance();

                }

                if (extractor != null) {
                    extractor.unselectTrack(index);
                    extractor.release();
                }

            }

            if (muxer != null) {
                muxer.stop();
                muxer.release();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
