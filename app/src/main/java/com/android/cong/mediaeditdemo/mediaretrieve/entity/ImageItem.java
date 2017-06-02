package com.android.cong.mediaeditdemo.mediaretrieve.entity;

import java.io.Serializable;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class ImageItem implements Serializable {
    public String path; // 图片路径
    public long time; // 图片所在的时间，毫秒

    public ImageItem() {
    }

    @Override
    public String toString() {
        return "ImageItem{path=" + path + ",time=" + time + "}";
    }
}
