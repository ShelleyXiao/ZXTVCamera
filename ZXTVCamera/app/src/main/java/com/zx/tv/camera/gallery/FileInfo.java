package com.zx.tv.camera.gallery;

import java.util.ArrayList;

/**
 * Created by BettySong on 11/23/15.
 */
public class FileInfo {
    private String mName;
    private String mPath;
    private String mParentFolderPath;
    private long mLastModified;
    private String mMineType;
    private Boolean mIsContinuePics = false;

    private boolean mIsSelected = false;

    private ArrayList<FileInfo> mContinuePics;

    public void setName(String name) {
        this.mName = name;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public void setParentFolderPath(String parentFolderPath) {
        this.mParentFolderPath = parentFolderPath;
    }

    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    public void setMineType(String mineType) {
        this.mMineType = mineType;
    }

    public void setIsContinuePics(Boolean isContinuePics) {
        this.mIsContinuePics = isContinuePics;
    }

    public void setIsSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public void setContinuePics(ArrayList<FileInfo> continuePics) {
        this.mContinuePics = continuePics;
    }

    public String getName() {
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    public String getParentFolderPath() {
        return mParentFolderPath;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public String getMineType() {
        return mMineType;
    }

    public Boolean isContinuePics() {
        return mIsContinuePics;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public ArrayList<FileInfo> getContinuePics() {
        return mContinuePics;
    }

}
