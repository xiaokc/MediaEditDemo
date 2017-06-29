package com.android.cong.mediaeditdemo.videomerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

/**
 * Created by xiaokecong on 07/06/2017.
 */

public class VideoMergeActivity extends Activity {
    private Button btnStart;

    private List<String> filePathList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);

        initView();

        initData();

    }

    private void initView() {
        btnStart = (Button) findViewById(R.id.btn_start);
        filePathList = new ArrayList<>();
    }

    private void initData() {
        // 视频
        filePathList.add(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/recordmaster/VideoEdit/20170602_170924_edited.mp4");

        filePathList.add(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/recordmaster/VideoEdit/20170602_170924_edited.mp4");

        // 音频
//        filePathList.add(Environment.getExternalStorageDirectory() + "/romatic.aac");
//        filePathList.add(Environment.getExternalStorageDirectory() + "/classic.aac");

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    new VideoMerger().mergeVideo(filePathList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 使用mp4parser进行视频合并
     *
     * @param filePathList
     */
    private void testMp4Parser(List<String> filePathList) {

        List<Movie> movies = new ArrayList<>();

        try {
            for (String filePath : filePathList) {
                movies.add(MovieCreator.build(filePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Track> videoTracks = new ArrayList<>();
        List<Track> audioTracks = new ArrayList<>();

        for (Movie movie : movies) {
            for (Track track : movie.getTracks()) {
                if (track.getHandler().equals("soun")) {
                    audioTracks.add(track);
                }
                if (track.getHandler().equals("vide")) {
                    videoTracks.add(track);
                }
            }
        }

        Movie outVideo = new Movie();
        try {
            if (audioTracks.size() > 0) {
                outVideo.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                outVideo.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Container outContainer = new DefaultMp4Builder().build(outVideo);

        try {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/recordmaster/out_merged.mp4";
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileChannel fc = new RandomAccessFile(filePath, "rw").getChannel();
            outContainer.writeContainer(fc);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        movies.clear();
    }

    /**
     * 以文件的形式进行拼接
     * mp4格式文件
     * 经测试，发现不行，只有第一个文件正常写入了，后面的无法拼接到第一个文件后面
     *
     * aac格式文件，不考虑头文件，直接进行文件拼接，可以
     * <p>
     *
     *
     * @param filePathList
     */
    private void filesMerge(List<String> filePathList) {
        try {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/recordmaster/out_merged.aac";
            File outFile = new File(filePath);
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            FileOutputStream fouts = new FileOutputStream(outFile);

            for (int i = 0; i < filePathList.size(); i++) {
                File file = new File(filePathList.get(i));
                FileInputStream fins = new FileInputStream(file);
                byte[] bytes = new byte[fins.available()];

                while (fins.read(bytes) != -1) {
                    fouts.write(bytes);
                }

                fouts.flush();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
