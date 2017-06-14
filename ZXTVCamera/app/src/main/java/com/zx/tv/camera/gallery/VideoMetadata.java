package com.zx.tv.camera.gallery;

import android.media.MediaMetadataRetriever;

import java.io.File;

/**
 * Created by BettySong on 12/2/15.
 */
public class VideoMetadata {
    private String mPath;
    private File mFile;
    private MediaMetadataRetriever mMetadata;

    VideoMetadata(String path) {
        mPath = path;
        mFile = new File(mPath);
        mMetadata = new MediaMetadataRetriever();
        mMetadata.setDataSource(mPath);
    }

    public String getTitle() {
        return mFile.getName();
    }

    public String getPath() {
        return mPath;
    }

    public String getTime() {
        return mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
    }

    public String getWidth() {
        return mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
    }

    public String getHeight() {
        return mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
    }

    public String getDuration() {
        String milliSecond = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return TimeHelper.secToTime(Integer.parseInt(milliSecond) / 1000);
    }

    public String getFileSize() {
        return FileHelper.getAutoFileOrFilesSize(mPath);
    }


}
