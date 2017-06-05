package com.android.cong.mediaeditdemo.recorder;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by xiaokecong on 05/06/2017.
 */

public class MyActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRecord();


    }

    private void startRecord() {
        Intent intent = new Intent(this, AdbShellRecodService.class);
        intent.putExtra("cmd","screenrecord --time-limit 20 --bit-rate 6000000 /sdcard/demo2.mp4");
        startService(intent);
    }
}
