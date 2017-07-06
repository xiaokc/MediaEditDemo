package com.android.cong.mediaeditdemo.videoeditor.adapter;

import java.util.List;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoFolderObject;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by xiaokecong on 05/07/2017.
 */

public class VideoFolderListAdapter extends RecyclerView.Adapter<VideoFolderListAdapter.ViewHolder> {
    private Context mContext;
    private List<VideoFolderObject> mDatas;
    private OnItemClickListener mListener;

    public VideoFolderListAdapter(Context context, List<VideoFolderObject> datas) {
        this.mContext = context;
        this.mDatas = datas;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_folder_classify, null, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        VideoFolderObject item = mDatas.get(position);
        holder.iv.setImageBitmap(item.getDirThumbBmp());
        holder.tvName.setText(item.getDirName());
        holder.tvCount.setText(String.valueOf(item.getFileCount()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(holder.itemView, holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv;
        private TextView tvName;
        private TextView tvCount;

        public ViewHolder(View itemView) {
            super(itemView);
            iv = (ImageView) itemView.findViewById(R.id.iv_folder_thumb);
            tvName = (TextView) itemView.findViewById(R.id.tv_folder_name);
            tvCount = (TextView) itemView.findViewById(R.id.tv_count);

        }

    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;

    }

}
