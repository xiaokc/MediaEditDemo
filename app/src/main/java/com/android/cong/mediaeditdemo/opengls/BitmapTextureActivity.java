package com.android.cong.mediaeditdemo.opengls;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by xiaokecong on 13/06/2017.
 */

public class BitmapTextureActivity extends Activity {
    private GLSurfaceView glSurfaceView;

    private boolean rendererSet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 =
                configurationInfo.reqGlEsVersion >= 0x20000
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                                    && (Build.FINGERPRINT.startsWith("generic")
                                                || Build.FINGERPRINT.startsWith("unknown")
                                                || Build.MODEL.contains("google_sdk")
                                                || Build.MODEL.contains("Emulator")
                                                || Build.MODEL.contains("Android SDK built for x86")));
        if (supportsEs2) {
            glSurfaceView.setEGLContextClientVersion(2);
            glSurfaceView.setRenderer(new BitmapTextureRender(this));
            rendererSet = true;

        } else {
            Toast.makeText(this, "不支持OpenGL ES 2.0", Toast.LENGTH_LONG).show();
            return;
        }
        setContentView(glSurfaceView);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }
}
