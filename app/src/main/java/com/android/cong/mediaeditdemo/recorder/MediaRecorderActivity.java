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
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by xiaokecong on 06/06/2017.
 * 使用MediaRecorder进行录屏
 * MediaProjection 是从android 5.0之后开放的屏幕采集接口，通过系统级服务MediaProjectionManager进行管理
 * 录屏过程分为两部分：
 * 1.通过MediaProjectionManager申请录屏权限，用户允许后开始录制屏幕；
 * 2.通过MediaRecorder对音视频数据进行处理
 */

public class MediaRecorderActivity extends Activity {
    private Button btnStart;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorderService recorderService;
    private DeviceUtil deviceUtil;

    private final int REQUEST_RECORD = 0x0;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x1;
    private final int REQUEST_RECORD_AUDIO = 0x2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        deviceUtil = new DeviceUtil(this);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setEnabled(false);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorderService.isRunning()) {
                    recorderService.stopRecord();
                    btnStart.setText("开始");
                    Toast.makeText(MediaRecorderActivity.this, "录屏结束", Toast.LENGTH_LONG).show();
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_RECORD);
                }
            }
        });

        // API 23开始，动态请求权限
        if (ContextCompat.checkSelfPermission(MediaRecorderActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
        }
        Intent intent = new Intent(this, MediaRecorderService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RECORD && resultCode == RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            recorderService.setMediaProjection(mediaProjection);
            recorderService.startRecord();
            btnStart.setText("停止");
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaRecorderService.RecordBinder binder = (MediaRecorderService.RecordBinder) service;
            recorderService = binder.getRecordService();
            recorderService.setConfig(deviceUtil.getScreenWidth(),
                    deviceUtil.getScreenHeight(), deviceUtil.getScreenDpi());
            btnStart.setEnabled(true);
            btnStart.setText(recorderService.isRunning() ? "停止" : "开始");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            recorderService = null;
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
        unbindService(connection);
    }
}
