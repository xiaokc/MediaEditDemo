package com.android.cong.mediaeditdemo.gpuimages.activity;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

/**
 * Created by xiaokecong on 16/06/2017.
 */

public class GpuImageMainActivity extends Activity implements View.OnClickListener{
    private Button btnGallery;
    private Button btnCamera;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpu_image_main);

        btnGallery = (Button) findViewById(R.id.btn_go_gallery);
        btnCamera = (Button) findViewById(R.id.btn_go_camera);

        btnGallery.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_go_gallery:
                startActivity(new Intent(GpuImageMainActivity.this,GalleryFilterActivity.class));
                break;
            case R.id.btn_go_camera:
                startActivity(new Intent(GpuImageMainActivity.this,CameraFilterActivity.class));
                break;
            default:
                break;
        }
    }
}
