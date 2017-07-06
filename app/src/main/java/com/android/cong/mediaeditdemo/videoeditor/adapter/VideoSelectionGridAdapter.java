package com.android.cong.mediaeditdemo.videoeditor.adapter;

import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoItem;
import com.android.cong.mediaeditdemo.videoeditor.ui.VideoThumbLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by xiaokecong on 04/07/2017.
 */

public class VideoSelectionGridAdapter extends RecyclerView.Adapter<VideoSelectionGridAdapter.ViewHolder> {
    private Context mContext;
    private List<VideoItem> mDatas;
    private LayoutInflater mInflater;

    private OnItemClickListener mListener;

    private VideoThumbLoader mVideoThumbLoader;

    public VideoSelectionGridAdapter(Context context, List<VideoItem> datas) {
        this.mContext = context;
        this.mDatas = datas;
        mInflater = LayoutInflater.from(mContext);
        mVideoThumbLoader = new VideoThumbLoader(mContext);


    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.item_grid_video, null, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final VideoItem item = mDatas.get(position);
//        Bitmap bitmap = Bitmap.createScaledBitmap(item.getThumbBmp(), (int) dp2px(48), (int) dp2px(48), true);
//        holder.iv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.format_media));
        holder.iv.setTag(item.getPath()); // ImageView和对应的视频path绑定
        mVideoThumbLoader.loadVideoThumb(item.getPath(),holder.iv);
        holder.tv.setText(item.getName());
        if (mListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(holder.itemView, holder.getAdapterPosition());
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public void setDatas(List<VideoItem> datas) {
        mDatas.clear();
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            iv = (ImageView) itemView.findViewById(R.id.iv_video_item);
            tv = (TextView) itemView.findViewById(R.id.tv_video_item);
        }
    }

    public float dp2px(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public interface OnItemClickListener{
        void onItemClick(View view,int position);
    }
}
