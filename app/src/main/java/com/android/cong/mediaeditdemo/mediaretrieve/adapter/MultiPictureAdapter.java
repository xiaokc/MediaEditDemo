package com.android.cong.mediaeditdemo.mediaretrieve.adapter;

import java.util.ArrayList;
import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.mediaretrieve.entity.ImageItem;
import com.bumptech.glide.Glide;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by xiaokecong on 02/06/2017.
 */

public class MultiPictureAdapter extends RecyclerView.Adapter<MultiPictureAdapter.ViewHolder> {

    private Context mContext;
    private List<ImageItem> mList = new ArrayList<>();
    private LayoutInflater mInflater;
    private int mItemWidth;

    public MultiPictureAdapter(Context context, int itemWidth) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mItemWidth = itemWidth;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.item_video_frame, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Glide.with(mContext)
                .load("file://" + mList.get(position).path)
                .into(holder.ivImage);

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = (ImageView) itemView.findViewById(R.id.iv_item);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) ivImage.getLayoutParams();
            params.width = mItemWidth;
            ivImage.setLayoutParams(params);
        }
    }

    public void addItemVideoInfo(ImageItem item) {
        mList.add(item);
        notifyItemInserted(mList.size());
    }
}
