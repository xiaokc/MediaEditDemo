package com.android.cong.mediaeditdemo.videoeditor.entity;

import android.graphics.Bitmap;

/**
 * Created by xiaokecong on 04/07/2017.
 */

public class VideoItem {
    private Bitmap thumbBmp;
    private String path;
    private String name;
    private String parentPath;

    private long size; // 视频大小，如20M
    private int width; // 视频宽、高
    private int height;
    private long duration; // 视频时长
    private long lastModifiedTime; // 视频上次修改时间

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Bitmap getThumbBmp() {
        return thumbBmp;
    }

    public void setThumbBmp(Bitmap thumbBmp) {
        this.thumbBmp = thumbBmp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VideoItem() {}
    public VideoItem(String path) {

        this.path = path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
}
