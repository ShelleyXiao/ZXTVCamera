package com.zx.album.task;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.zx.album.entity.AlbumImage;

import java.util.ArrayList;
import java.util.List;


public class FileLoader extends AsyncTaskLoader<List<AlbumImage>> {
    private List<AlbumImage> dataResult;
    private boolean dataIsReady = false;
//    public static final String CameraPath = Storage.DIRECTORY;
    private String mPath;

    public FileLoader(Context context) {
        super(context);
        setDefaultPath();
        load();
    }

    public FileLoader(Context context, String path) {
        super(context);
        setPath(path);
        load();
    }

    private void load() {
        if (dataIsReady) {
            deliverResult(dataResult);
        } else {
            forceLoad();
        }
    }

    public void setDefaultPath() {
//        mPath = CameraPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    @Override
    public List<AlbumImage> loadInBackground() {
        ArrayList<AlbumImage> list = AlbumImageManager.getFileInfoList(mPath);
        return list;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        //show the loading view
        super.onStartLoading();
    }

    @Override
    protected void onStopLoading() {
        // hide the loading view
        super.onStopLoading();
    }

    @Override
    public boolean takeContentChanged() {
        return super.takeContentChanged();
    }


}
