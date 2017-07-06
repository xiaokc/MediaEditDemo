package com.android.cong.mediaeditdemo.videoeditor.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.adapter.VideoFolderListAdapter;
import com.android.cong.mediaeditdemo.videoeditor.adapter.VideoSelectionGridAdapter;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoFolderObject;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoItem;
import com.android.cong.mediaeditdemo.videoeditor.ui.CustomPopWindow;
import com.android.cong.mediaeditdemo.videoeditor.ui.VideoQueryTask;
import com.android.cong.mediaeditdemo.videoeditor.utils.VideoQueryManager;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by xiaokecong on 04/07/2017.
 */

public class VideoSelectionActivity extends BaseActivity {
    private Toolbar toolbar;
    private RelativeLayout selectVideoLayout;
    private TextView tvSelectVideo;
    private RecyclerView videosRecyclerView;

    private VideoSelectionGridAdapter adapter;

    private CustomPopWindow listPopWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_selection);

        initView();

        showData();
    }

    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("视频选择");
        }

        toolbar.setNavigationIcon(R.drawable.chevron_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 04/07/2017  回到首页
                finish();
            }
        });

        selectVideoLayout = (RelativeLayout) findViewById(R.id.rl_select_video);
        tvSelectVideo = (TextView) findViewById(R.id.tv_select_video);

        videosRecyclerView = (RecyclerView) findViewById(R.id.rv_videos);
        videosRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));

    }

    private void showData() {
        final List<VideoItem> list = getVideoDataList();
        VideoQueryTask task = new VideoQueryTask(this);
        task.execute();
        task.setOnVideoQueryDoneListener(new VideoQueryTask.OnVideoQueryDoneListener() {
            @Override
            public void onDone(final List<VideoItem> videoItems) {
                adapter = new VideoSelectionGridAdapter(VideoSelectionActivity.this, videoItems);
                videosRecyclerView.setAdapter(adapter);
                adapter.setOnItemClickListener(new VideoSelectionGridAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(VideoSelectionActivity.this);
                        AlertDialog dialog = builder.setMessage("视频过大，需要剪切后编辑")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.i("===>xkc","点击确定");
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.i("===>xkc","点击取消");
                                        dialog.dismiss();
                                    }
                                })
                                .show();

                    }
                });
            }
        });


//        selectVideoLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (listPopWindow != null) {
//                    listPopWindow.dissmiss();
//                    listPopWindow = null;
//                } else {
//                    showFolderClassifyList();
//                }
//            }
//        });
    }

    private void showFolderClassifyList() {
        View contentView = LayoutInflater.from(this).inflate(R.layout.pop_folder_classify, null, false);
        RecyclerView videoClassifyRecyclerView = (RecyclerView) contentView.findViewById(R.id.rv_video_folder);
        videoClassifyRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        final List<VideoFolderObject> datas = getClassifyFolderList();
        VideoFolderListAdapter listAdapter = new VideoFolderListAdapter(this, datas);
        videoClassifyRecyclerView.setAdapter(listAdapter);
        listAdapter.setItemClickListener(new VideoFolderListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                VideoFolderObject item = datas.get(position);
                Log.i("===>xkc", "item:" + item.getDirName() + "," + item.getFileCount());
                tvSelectVideo.setText(item.getDirName());
                if (listPopWindow != null) {
                    listPopWindow.dissmiss();
                    listPopWindow = null;
                }

            }
        });

        // 显示list弹窗
        listPopWindow = new CustomPopWindow.PopupWindowBuilder(this)
                .setView(contentView)
                .size(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .create()
                .showAsDropDown(selectVideoLayout);

    }

    private List<VideoItem> getVideoDataList() {
        List<VideoItem> datas = VideoQueryManager.getAllVideos();
        if (datas != null && datas.size() > 0) {
            return datas;
        }

        datas = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            VideoItem item = new VideoItem();
            item.setName("test.mp4");
            item.setThumbBmp(BitmapFactory.decodeResource(getResources(), R.drawable.format_media));
            datas.add(item);
        }

        return datas;
    }

    private List<VideoFolderObject> getClassifyFolderList() {
        List<VideoFolderObject> datas = new ArrayList<>();
//        if (datas != null && datas.size() > 0) {
//            return datas;
//        }
        Map<String, List<VideoItem>> listMap = VideoQueryManager.getAllVideoMap();
        if (null == listMap || listMap.size() <= 0) {
            return null;
        }
        for (Map.Entry<String, List<VideoItem>> entry : listMap.entrySet()) {
            String dirPath = entry.getKey();
            List<VideoItem> list = entry.getValue();

            VideoFolderObject object = new VideoFolderObject();
            object.setDirName(new File(dirPath).getName());
            object.setFileCount(list.size());
            object.setDirThumbBmp(list.get(0).getThumbBmp());

            datas.add(object);

        }
        //        for (int i = 0; i < 6;i ++) {
        //            VideoFolderObject item = new VideoFolderObject();
        //            item.setDirName("微信");
        //            item.setDirThumbBmp(BitmapFactory.decodeResource(getResources(),R.drawable.format_media));
        //            item.setFileCount(25);
        //            datas.add(item);
        //        }

        return datas;
    }

}
