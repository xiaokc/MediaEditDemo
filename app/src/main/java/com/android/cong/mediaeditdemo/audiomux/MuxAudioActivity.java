package com.android.cong.mediaeditdemo.audiomux;

import java.io.IOException;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

/**
 * Created by xiaokecong on 16/06/2017.
 */

public class MuxAudioActivity extends Activity {
    private Button btnMux;
    private final String AUDIO_1 = Environment.getExternalStorageDirectory() + "/romatic.aac";
    private final String AUDIO_2 = Environment.getExternalStorageDirectory() + "/classic.aac";

    private String decodePath1 = Environment.getExternalStorageDirectory() + "/romatic_d.pcm";
    private String decodePath2 = Environment.getExternalStorageDirectory() + "/classic_d.pcm";

    private String decodePath = Environment.getExternalStorageDirectory() + "/mixed.mp3";

    private int channelCnt;
    private int sampleRate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mux_audio);
        btnMux = (Button) findViewById(R.id.btn_mux);

        btnMux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                 startMux(AUDIO_1, AUDIO_2);
                MuxHelper.ComposeAudio(decodePath1, decodePath2, decodePath, false, 0.2f, 0.8f, 44100, null, 44100);
            }
        });
    }

    private void startMux(final String audio1, final String audio2) {
        final long[] decodeRes1 = decodeAudio(audio1);
        final long[] decodeRes2 = decodeAudio(audio2);

        // 以时长较短的为基准
        if (decodeRes1[2] <= decodeRes2[2]) {
            channelCnt = (int) decodeRes1[0];
            sampleRate = (int) decodeRes1[1];
        } else {
            channelCnt = (int) decodeRes2[0];
            sampleRate = (int) decodeRes2[1];
        }

        new Thread(){
            @Override
            public void run() {
                MuxHelper.decodeMusicFile(audio1, decodePath1, 0, (int) decodeRes1[2], channelCnt, sampleRate);
                MuxHelper.decodeMusicFile(audio2, decodePath2, 0, (int) decodeRes2[2], channelCnt, sampleRate);
            }
        }.start();

    }

    private long[] decodeAudio(String audio) {
        long[] res = new long[3];
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(audio);

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    res[0] = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT); // 通道数
                    res[1] = format.getInteger(MediaFormat.KEY_SAMPLE_RATE); // 采样率
                    res[2] = format.getLong(MediaFormat.KEY_DURATION); // 时长
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }
}
