package com.android.cong.mediaeditdemo.recorder;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.mediaretrieve.utils.DeviceUtil;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by xiaokecong on 06/06/2017.
 */

public class MediaCodecActivity extends Activity {
    private Button btnStart;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaCodecService mediaCodecService;
    private DeviceUtil deviceUtil;

    private final int REQUEST_CAPTURE_CODE = 0x0;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x1;
    private final int REQUEST_RECORD_AUDIO = 0x2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        deviceUtil = new DeviceUtil(this);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaCodecService.isRunning()) {
                    mediaCodecService.stopRecord();
                    btnStart.setText("开始");
                    Toast.makeText(MediaCodecActivity.this, "录屏结束", Toast.LENGTH_LONG).show();
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CAPTURE_CODE);
                }
            }
        });

        // API 23开始，动态请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
        }

        Intent intent = new Intent(this, MediaCodecService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_CODE && resultCode == RESULT_OK) {
            Log.i("===>xkc", "权限获取成功");
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            mediaCodecService.setMediaProjection(mediaProjection);
            mediaCodecService.startRecord();
            btnStart.setText("停止");

        }

    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaCodecService.MediaCodecBinder binder = (MediaCodecService.MediaCodecBinder) service;
            mediaCodecService = binder.getMediaCodecService();
            mediaCodecService.setConfig(deviceUtil.getScreenWidth(),
                    deviceUtil.getScreenHeight(), deviceUtil.getScreenDpi(), Environment.getExternalStorageDirectory
                            () + "/recordmaster/out_recorded.mp4");
            btnStart.setText(mediaCodecService.isRunning()?"停止":"开始");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaCodecService = null;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE || requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }
}
