package com.android.cong.mediaeditdemo.videoeditor.fragment;

import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoItem;
import com.android.cong.mediaeditdemo.videoeditor.utils.VideoQueryManager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by xiaokecong on 05/07/2017.
 */

public class EditedVideoFragment extends Fragment {
    private RecyclerView editedVideoRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edited_video_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showData();
    }

    private void initView(View view) {
        editedVideoRecyclerView = (RecyclerView) view.findViewById(R.id.rv_edited_video_list);
        editedVideoRecyclerView
                .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

    }

    private void showData() {
        List<VideoItem> datas = VideoQueryManager.getAllVideos();

    }

    class EditedVideoListAdapter extends RecyclerView.Adapter<ViewHolder> {
        List<VideoItem> mDatas;

        EditedVideoListAdapter(List<VideoItem> datas) {
            this.mDatas = datas;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View itemView = LayoutInflater.from()
//            ViewHolder holder = new ViewHolder()
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName;
        TextView tvSizeAndDate;
        TextView tvWidthAndHeight;
        TextView tvDuration;

        ViewHolder(View itemView) {
            super(itemView);
            ivThumb = (ImageView) itemView.findViewById(R.id.iv_edited_thumb);
            tvName = (TextView) itemView.findViewById(R.id.tv_edited_name);
            tvSizeAndDate = (TextView) itemView.findViewById(R.id.tv_size_date);
            tvWidthAndHeight = (TextView) itemView.findViewById(R.id.tv_width_height);
            tvDuration = (TextView) itemView.findViewById(R.id.tv_duration);
        }
    }
}
