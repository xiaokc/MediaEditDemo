package com.android.cong.mediaeditdemo.videoeditor.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.cong.mediaeditdemo.R;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoFolderObject;
import com.android.cong.mediaeditdemo.videoeditor.entity.VideoItem;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

/**
 * Created by xiaokecong on 05/07/2017.
 */

public class VideoQueryManager {

    private static List<VideoItem> allVideos;
    private static Map<String, List<VideoItem>> allVideoByDir;
    private static List<VideoFolderObject> allVideosWithDir;

    public static void init(Context context) {
        allVideos = queryAllVideos(context);
        allVideoByDir = classifyVideo();
        allVideosWithDir = queryAllVideosWithParentDir(context);
    }

    private static List<VideoItem> queryAllVideos(Context context) {
        List<VideoItem> list = new ArrayList<>();
        final String[] mProjection = new String[] {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION

        };
        Cursor cursor =
                context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mProjection, null,
                        null, MediaStore.Video.Media.DATE_MODIFIED + " desc");
        if (null == cursor || cursor.getColumnCount() <= 0) {
            return null;
        }
        cursor.moveToFirst();
        String name;
        String path;
        Bitmap thumb;

        long size;
        int width;
        int height;
        long lastModifiedTime;
        long duration;

        while (cursor.moveToNext()) {
            VideoItem item = new VideoItem();
            name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
            lastModifiedTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
            width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH));
            height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT));
            duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));


//            thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
//            if (null == thumb) {
//                thumb = BitmapFactory.decodeResource(context.getResources(), R.drawable.format_media);
//            }
            item.setName(name);
            item.setPath(path);
//            item.setThumbBmp(thumb);
            item.setSize(size);
            item.setLastModifiedTime(lastModifiedTime);
            item.setWidth(width);
            item.setHeight(height);
            item.setDuration(duration);

            list.add(item);

        }
        if (cursor != null) {
            cursor.close();
        }

        return list;

    }

    private static List<VideoFolderObject> queryAllVideosWithParentDir(Context context) {
        List<VideoFolderObject> list = new ArrayList<>();

        String numOfVideos = "num_of_videos";

        /** 要从MediaStore检索的列 */
        final String[] mProjection = new String[] {
                MediaStore.Files.FileColumns.DATA,
                "count(" + MediaStore.Files.FileColumns.PARENT + ") as " + numOfVideos,
                MediaStore.Files.FileColumns.DISPLAY_NAME
        };
        /** where子句 */
        String mSelection = MediaStore.Files.FileColumns.MEDIA_TYPE + " = "
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ")"
                + " group by (" + MediaStore.Files.FileColumns.PARENT;

        //查询子句:select _data,count(parent) as num_of_songs from file_table where ( media_type = 2) group by (parent);
        Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), mProjection,
                mSelection, null, null);

        if (null == cursor || cursor.getColumnCount() <= 0) {
            return null;
        }

        String folderName;
        String folderPath;
        int videoCount;
        Bitmap folderThumb;

        while (cursor.moveToNext()) {
            VideoFolderObject object = new VideoFolderObject();
            folderPath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
            videoCount = cursor.getInt(cursor.getColumnIndex(numOfVideos));
            folderName = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME));
            folderThumb = ThumbnailUtils.createVideoThumbnail(folderPath, MediaStore.Images.Thumbnails.MICRO_KIND);

            object.setFileCount(videoCount);
            object.setDirName(folderName);
            object.setDirPath(folderPath);
            object.setDirThumbBmp(folderThumb);

            list.add(object);
        }

        return list;
    }

    public static List<VideoItem> getAllVideos() {
        return allVideos;
    }

    private static Map<String, List<VideoItem>> classifyVideo() {
        Map<String, List<VideoItem>> listMap = new HashMap<>();
        for (VideoItem item : allVideos) {
            String path = item.getPath();
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    String parentPath = file.getParent();
                    if (null == parentPath) {
                        parentPath = "其他"; // 该视频文件没有父目录
                    }
                    if (listMap.containsKey(parentPath)) {
                        List<VideoItem> list = listMap.get(parentPath);
                        list.add(item);
                        listMap.put(parentPath, list);
                    } else { // 没有该分类目录
                        List<VideoItem> list = new ArrayList<>();
                        list.add(item);
                        listMap.put(parentPath, list);
                    }
                }

            }
        }
        return listMap;
    }

    public static Map<String, List<VideoItem>> getAllVideoMap() {
        return allVideoByDir;
    }

    public static List<VideoFolderObject> getAllVideosWithDir() {
        return allVideosWithDir;
    }

    public static List<VideoItem> getVideoByDir(String dirPath) {
        return allVideoByDir.get(dirPath);
    }

}
