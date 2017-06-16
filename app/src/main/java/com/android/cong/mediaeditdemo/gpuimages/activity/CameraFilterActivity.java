package com.android.cong.mediaeditdemo.gpuimages.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.gpuimages.util.CameraHelper;
import com.android.cong.mediaeditdemo.gpuimages.util.GPUImageFilterTools;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

/**
 * 使用GpuImage对相机预览实时滤镜
 * Created by xiaokecong on 16/06/2017.
 */

public class CameraFilterActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private GPUImage mGPUImage;
    private CameraHelper mCameraHelper;
    private CameraLoader mCamera;
    private GPUImageFilter mFilter;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;

    private SeekBar seekBar;
    private Button btnChooseFilter;
    private ImageButton btnCapture;
    private ImageView imgSwitchCamera;

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter);

        seekBar = (SeekBar) findViewById(R.id.seekbar_camera);
        btnChooseFilter = (Button) findViewById(R.id.btn_choose_filter);
        btnCapture = (ImageButton) findViewById(R.id.btn_capture);
        imgSwitchCamera = (ImageView) findViewById(R.id.iv_switch_camera);

        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.camera_surfaceview);

        mGPUImage = new GPUImage(this);
        mGPUImage.setGLSurfaceView(mGLSurfaceView);

        mCameraHelper = new CameraHelper(this);
        mCamera = new CameraLoader();

        seekBar.setOnSeekBarChangeListener(this);
        btnCapture.setOnClickListener(this);
        btnChooseFilter.setOnClickListener(this);
        imgSwitchCamera.setOnClickListener(this);

        // 没有摄像头
        if (!mCameraHelper.hasFrontCamera() || !mCameraHelper.hasBackCamera()) {
            imgSwitchCamera.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.onResume();
    }

    @Override
    protected void onPause() {
        mCamera.onPause();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_choose_filter:
                GPUImageFilterTools.showDialog(this, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {
                    @Override
                    public void onGpuImageFilterChosenListener(GPUImageFilter filter) {
                        switchFilterTo(filter);
                    }
                });
                break;
            case R.id.btn_capture:
                if (mCamera.mCameraInstance.getParameters().getFocusMode().equals(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    takePicture();
                } else {
                    mCamera.mCameraInstance.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            takePicture();
                        }
                    });
                }
                break;
            case R.id.iv_switch_camera:
                mCamera.switchCamera();
                break;
            default:
                break;
        }

    }

    private void takePicture() {
        final Camera.Parameters parameters = mCamera.mCameraInstance.getParameters();
        parameters.setRotation(90);
        mCamera.mCameraInstance.setParameters(parameters);
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            Log.i("===>xkc", "支持图片尺寸：" + size.width + "X" + size.height);
        }
        mCamera.mCameraInstance.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, final Camera camera) {
                final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (null == pictureFile) {
                    Log.d("===>xkc", "拍照失败，检查存储卡权限");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.e("===>xkc", "file not found:" + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e("===>xkc", "error accessing file:" + e.getMessage());
                    e.printStackTrace();
                }

                data = null;

                Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
                mGPUImage.setImage(bitmap);

                mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                mGPUImage.saveToPictures(bitmap, "GPUImage", System.currentTimeMillis() + ".jpg", new GPUImage
                        .OnPictureSavedListener() {
                    @Override
                    public void onPictureSaved(Uri uri) {
                        pictureFile.delete();
                        camera.startPreview();
                        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                        Toast.makeText(CameraFilterActivity.this,"保存成功",Toast.LENGTH_LONG).show();
                    }
                });

            }
        });
    }

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private static File getOutputMediaFile(int type) {
        // TODO: 安全起见，应该先检查存储卡是否挂载的状态：Environment.getExternalStorageState()
        File mediaStorageDir = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "GPUImageApp");

        // TODO: 图片的分享动作可以放到这个位置来做
        /*图片分享*/

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("===>xkc", "创建文件夹失败");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;

    }

    private void switchFilterTo(GPUImageFilter filter) {
        if (null == mFilter || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImage.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class CameraLoader {
        private int mCurrentCameraId = 0;
        private Camera mCameraInstance;

        private void onResume() {
            setUpCamera(mCurrentCameraId);
        }

        private void onPause() {
            releaseCamera();
        }

        private void switchCamera() {
            releaseCamera();
            mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
            setUpCamera(mCurrentCameraId);
        }

        private void setUpCamera(int id) {
            mCameraInstance = getInstance(id);
            Camera.Parameters parameters = mCameraInstance.getParameters();

            // TODO adjust by getting supportedPreviewSizes and choose the best one for screen size(best fill screen)
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCameraInstance.setParameters(parameters);

            int orientation = mCameraHelper.getCameraDisplayOrientation(CameraFilterActivity.this, mCurrentCameraId);
            CameraHelper.CameraInfo2 cameraInfo2 = new CameraHelper.CameraInfo2();
            mCameraHelper.getCameraInfo(mCurrentCameraId, cameraInfo2);
            boolean flipHorizontal = cameraInfo2.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
            mGPUImage.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
        }

        // 获取相机实例的安全做法
        private Camera getInstance(int id) {
            Camera c = null;
            try {
                c = mCameraHelper.openCamera(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return c;

        }

        private void releaseCamera() {
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.release();
            mCameraInstance = null;
        }

    }
}
