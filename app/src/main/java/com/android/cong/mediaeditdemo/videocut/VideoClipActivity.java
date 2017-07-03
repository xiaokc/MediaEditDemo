package com.android.cong.mediaeditdemo.videocut;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

/**
 * Created by xiaokecong on 07/06/2017.
 */

public class VideoClipActivity extends Activity {
    private Button btnClip;

    private final String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .getAbsolutePath() + "/Video/V70602-103441.mp4";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_clip);

        btnClip = (Button) findViewById(R.id.btn_clip);
        btnClip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new VideoCliper().decodeVideo(filePath, 13000000, 3000000);
            }
        });
    }
}
