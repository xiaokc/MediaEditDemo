package com.android.cong.mediaeditdemo.gpuimages.activity;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.gpuimages.util.GPUImageFilterTools;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 * 本地图片使用GpuImage进行滤镜
 * Created by xiaokecong on 16/06/2017.
 */

public class GalleryFilterActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,
        GPUImageView.OnPictureSavedListener {
    private SeekBar seekBar;
    private Button btnFilterChoose;
    private Button btnSave;

    private static final int REQUEST_PICK_IMAGE = 1;
    private GPUImageFilter mFilter;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;
    private GPUImageView mGPUImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_filter);
        initView();

        pickImage();
    }

    private void initView() {
        mGPUImageView = (GPUImageView) findViewById(R.id.gpuimage);
        seekBar = (SeekBar) findViewById(R.id.seekbar_gallery);
        btnFilterChoose = (Button) findViewById(R.id.btn_choose_filter);
        btnSave = (Button) findViewById(R.id.btn_save);

        seekBar.setOnSeekBarChangeListener(this);
        btnFilterChoose.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    private void pickImage() {
        Intent photoPickIntent = new Intent(Intent.ACTION_PICK);
        photoPickIntent.setType("image/*");
        startActivityForResult(photoPickIntent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    handleImage(data.getData());
                } else {
                    Toast.makeText(GalleryFilterActivity.this, "打开相册失败", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;

        }

    }

    private void handleImage(Uri uri) {
        mGPUImageView.setImage(uri);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_choose_filter:
                GPUImageFilterTools.showDialog(this, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {
                    @Override
                    public void onGpuImageFilterChosenListener(GPUImageFilter filter) {
                        switchFilterTo(filter);
                        mGPUImageView.requestRender();
                    }
                });
                break;
            case R.id.btn_save:
                saveImage();
                break;
            default:
                break;
        }

    }

    private void switchFilterTo(GPUImageFilter filter) {
        if (null == mFilter || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);

            seekBar.setVisibility(mFilterAdjuster.canAdjust() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }

        mGPUImageView.requestRender();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void saveImage() {
        String fileName = System.currentTimeMillis() +".jpg";
        mGPUImageView.saveToPictures("GPUImage",fileName, this);
    }

    @Override
    public void onPictureSaved(Uri uri) {
        Toast.makeText(this, "saved:" + uri.toString(), Toast.LENGTH_LONG).show();
    }
}
