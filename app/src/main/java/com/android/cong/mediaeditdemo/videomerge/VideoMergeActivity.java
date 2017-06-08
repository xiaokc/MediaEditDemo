package com.android.cong.mediaeditdemo.videomerge;

import java.io.File;
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
        filePathList.add(Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/recordmaster/VideoEdit/20170601_164328_edited.mp4");

        filePathList.add(Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/recordmaster/VideoEdit/20170601_171122_edited.mp4");

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testMp4Parser(filePathList);
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
                    +"/recordmaster/out_merged.mp4";
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileChannel fc = new RandomAccessFile(filePath,"rw").getChannel();
            outContainer.writeContainer(fc);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        movies.clear();
    }


}
