package com.android.cong.mediaeditdemo.mediaretrieve;

import java.io.File;
import java.lang.ref.WeakReference;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.mediaretrieve.adapter.MultiPictureAdapter;
import com.android.cong.mediaeditdemo.mediaretrieve.entity.ImageItem;
import com.android.cong.mediaeditdemo.mediaretrieve.utils.DeviceUtil;
import com.android.cong.mediaeditdemo.mediaretrieve.utils.ExtractVideoFrameThread;
import com.android.cong.mediaeditdemo.mediaretrieve.utils.PictureUtil;
import com.android.cong.mediaeditdemo.mediaretrieve.utils.VideoInfoUtil;
import com.bumptech.glide.Glide;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class MediaInfoRetrieveActivity extends Activity implements View.OnClickListener {
    private Button btnGetInfo;
    private TextView tvInfo;

    private Button btnGetCover;
    private ImageView ivCover;

    private Button btnGetPicture;

    private VideoInfoUtil mVideoInfoUtil;

    private RecyclerView mRecyclerView;
    private MultiPictureAdapter mAdapter;
    private ExtractVideoFrameThread mThread;
    private MainHandler mHandler;

    private final String mVideoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .getAbsolutePath() + "/Video/V70602-103441.mp4";
    private final String mOutputFileDirPath = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM).getAbsolutePath() + "/Extract";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_info_retrieve);

        initView();

        initData();

    }

    private void initView() {
        btnGetInfo = (Button) findViewById(R.id.btn_get_info);
        tvInfo = (TextView) findViewById(R.id.tv_info);
        btnGetCover = (Button) findViewById(R.id.btn_get_cover);
        ivCover = (ImageView) findViewById(R.id.iv_cover);
        btnGetPicture = (Button) findViewById(R.id.btn_get_pictures);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 5);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new MultiPictureAdapter(this, DeviceUtil.getScreenWidth(this) / 5);
        mRecyclerView.setAdapter(mAdapter);

        btnGetInfo.setOnClickListener(this);
        btnGetCover.setOnClickListener(this);
        btnGetPicture.setOnClickListener(this);
    }

    private void initData() {
        if (!new File(mVideoPath).exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_LONG).show();
            finish();
        }
        mVideoInfoUtil = new VideoInfoUtil(mVideoPath);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_get_info:
                getVideoInfo();
                break;
            case R.id.btn_get_cover:
                getVideoFirstFrameBitmap();
                break;
            case R.id.btn_get_pictures:
                getVideoMultiPictures();
                break;
            default:
                break;
        }

    }

    private void getVideoInfo() {
        String videoLength = mVideoInfoUtil.getVideoLength();
        long videoWidth = mVideoInfoUtil.getVideoWidth();
        long videoHeight = mVideoInfoUtil.getVideoHeight();
        long videoRotation = mVideoInfoUtil.getVideoRotateDegree();
        String videoMimeType = mVideoInfoUtil.getVideoMimeType();

        StringBuilder builder = new StringBuilder();
        builder.append("duration:").append(videoLength).append("ms\n")
                .append("width:").append(videoWidth).append("\n")
                .append("height:").append(videoHeight).append("\n")
                .append("degree:").append(videoRotation).append("\n")
                .append("mimetype:").append(videoMimeType);

        tvInfo.setText(builder.toString());

    }

    private void getVideoFirstFrameBitmap() {
        Bitmap bitmap = mVideoInfoUtil.getFrameAtTime(0);
//        if (bitmap != null) {
//            ivCover.setImageBitmap(bitmap);
//        }

        String path = PictureUtil.saveImageToSD(bitmap, mOutputFileDirPath);
        Glide.with(this).load("file://"+path).into(ivCover);
    }

    private void getVideoMultiPictures() {
        long endPosition = Long.valueOf(mVideoInfoUtil.getVideoLength());
        long startPosition = 0;
        int thumbnailsCount = 10;
        int imgWidth = DeviceUtil.getScreenWidth(this) / 5;
        int imgHeight = DeviceUtil.dip2px(this, 55);
        mHandler = new MainHandler(this);
        mThread = new ExtractVideoFrameThread(mVideoPath, mOutputFileDirPath, imgWidth, imgHeight, startPosition,
                endPosition, thumbnailsCount, mHandler);
        mThread.start();

    }

    private static class MainHandler extends Handler {
        private final WeakReference<MediaInfoRetrieveActivity> mActivity;

        MainHandler(MediaInfoRetrieveActivity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaInfoRetrieveActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == 0) {
                    if (activity.mAdapter != null) {
                        ImageItem item = (ImageItem) msg.obj;
                        activity.mAdapter.addItemVideoInfo(item);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoInfoUtil.destroy();
    }
}
