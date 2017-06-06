package com.android.cong.mediaeditdemo.recorder;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by xiaokecong on 05/06/2017.
 */

public class AdbShellRecordActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startAdbShellRecord();

    }

    /**
     * 使用adb shell screenrecord命令进行录屏
     */
    private void startAdbShellRecord() {
        Intent intent = new Intent(this, AdbShellRecodService.class);
        startService(intent);
    }
}
