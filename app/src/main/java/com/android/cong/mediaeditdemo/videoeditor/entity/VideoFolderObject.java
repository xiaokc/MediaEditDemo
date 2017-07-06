package com.android.cong.mediaeditdemo.videoeditor.entity;

import android.graphics.Bitmap;

/**
 * Created by xiaokecong on 05/07/2017.
 */

public class VideoFolderObject {
    private String dirPath;
    private String dirName;
    private Bitmap dirThumbBmp;
    private int fileCount;

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public Bitmap getDirThumbBmp() {
        return dirThumbBmp;
    }

    public void setDirThumbBmp(Bitmap dirThumbBmp) {
        this.dirThumbBmp = dirThumbBmp;
    }
}
