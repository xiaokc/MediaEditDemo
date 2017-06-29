package com.android.cong.mediaeditdemo.surfaces;

import java.io.IOException;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;

/**
 * Created by xiaokecong on 11/06/2017.
 */

public class CameraPreviewTextureViewActivity extends Activity implements TextureView.SurfaceTextureListener {
    private TextureView textureView;
    private Camera camera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        setContentView(textureView);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        camera = Camera.open();
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        textureView
                .setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER));
        try {
            camera.setPreviewTexture(surface);
        } catch (IOException e) {
            Log.e("===>xkc", "camera.setPreviewTexture error");
            e.printStackTrace();
        }

        camera.startPreview();
        textureView.setAlpha(1.0f);
        textureView.setRotation(90.0f);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.stopPreview();
        camera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
