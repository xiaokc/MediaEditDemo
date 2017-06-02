package com.android.cong.mediaeditdemo.mediaretrieve.utils;

import android.os.Handler;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class ExtractVideoFrameThread extends Thread {
    private String videoPath;
    private String outputFileDirPath;
    private long startPosition;
    private long endPosition;
    private int thumbnailCount;
    private ExtractVideoFrameAsyncUtil extractVideoFrameAsyncUtil;
    public ExtractVideoFrameThread(String videoPath, String ouputFileDirPath, int extractW, int extractH,
                                   long startPosition, long endPosition, int thumbnailCount, Handler handler) {
        this.videoPath = videoPath;
        this.outputFileDirPath = ouputFileDirPath;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.thumbnailCount = thumbnailCount;

        this.extractVideoFrameAsyncUtil = new ExtractVideoFrameAsyncUtil(extractW,extractH,handler);
    }

    @Override
    public void run() {
        super.run();
        extractVideoFrameAsyncUtil.getVideoFrame(videoPath, outputFileDirPath, startPosition, endPosition, thumbnailCount);
    }
}
