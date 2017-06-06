package com.android.cong.mediaeditdemo.mediaretrieve.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class DeviceUtil {
    private int screenWidth;
    private int screenHeight;
    private int screenDpi;
    private Context context;

    public DeviceUtil(Context context) {
        this.context = context;
        getScreenData();
    }

    public void getScreenData() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        screenDpi = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

    }
    public  int getScreenWidth() {
        return screenWidth;
    }

    public  int getScreenHeight(){
        return screenHeight;
    }

    public int getScreenDpi() {
        return screenDpi;
    }

    public static int dip2px(Context context, int dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5f);
    }
}
