package com.android.cong.mediaeditdemo.videoeditor.activity;

import com.android.cong.mediaeditdemo.videoeditor.utils.VideoQueryManager;
import com.android.cong.mediaeditdemo.videomux.AndroidUtil;

import android.app.Application;

/**
 * Created by xiaokecong on 05/07/2017.
 */

public class BaseApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
//        VideoQueryManager.init(this);
//        AndroidUtil.init(this);
    }
}
