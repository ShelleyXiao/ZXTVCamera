package com.zx.tv.camera.gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.zx.tv.camera.R;
import com.zx.tv.camera.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class ContinuePicsActivity extends Activity implements
        View.OnClickListener,
        Gallery.OnItemSelectedListener,
        LoaderManager.LoaderCallbacks<List<FileInfo>> {

    private static final String TAG = "com.zhaoxin.zxcamera.gallery.ContinuePicsActivity";
    private PicGallery mGallery;
    private Button mBack;
    private Button mSaveSome;
    private Button mSaveAll;
    private TextView mStatistics;
    private int mCurrentGalleryPosition = 0;
    private GalleryAdapter mAdapter;
    private String mPath;
    private List<FileInfo> mFileList;
    private int mSelectedNum = 0;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.continue_pics);

        Bundle bundle = getIntent().getExtras();
        mPath = bundle.getString("PATH");

        init();

        getLoaderManager().initLoader(0, null, this);

        initSize();

        initImageLoader();
    }

    private void init() {
        mBack = (Button) findViewById(R.id.back_to_gallery);
        mBack.setOnClickListener(this);
        mSaveAll = (Button) findViewById(R.id.save_all);
        mSaveAll.setOnClickListener(this);
        mSaveSome = (Button) findViewById(R.id.save_some);
        mSaveSome.setOnClickListener(this);
        mStatistics = (TextView) findViewById(R.id.counter);

        mGallery = (PicGallery) findViewById(R.id.pic_gallery_continues);
        mGallery.setVerticalFadingEdgeEnabled(true);
        mGallery.setHorizontalFadingEdgeEnabled(true);
        mAdapter = new GalleryAdapter(this);
        mAdapter.showSelectedView(true);

        mGallery.setAdapter(mAdapter);
        mGallery.setDetector(new GestureDetector(this, new MySimpleGesture()));
        mGallery.setOnItemSelectedListener(this);
    }

    private void initImageLoader() {
        mImageLoader.init(ImageLoaderConfiguration.createDefault(this));
        mAdapter.setImageLoader(mImageLoader);
    }

    public void initSize() {
        int screenWidth = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        mGallery.setScreenSize(screenWidth, screenHeight);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_to_gallery:
                this.finish();
                break;

            case R.id.save_all:
                saveAll();
                this.finish();
                break;
            case R.id.save_some:
                if (mSelectedNum == 0) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.warning)
                            .setPositiveButton(R.string.confirm, null)
                            .setMessage(R.string.nothing)
                            .show();
                    break;
                }
                saveSome();
                this.finish();
                break;
            default:
                break;
        }
    }

    private void saveSome() {
        Boolean res;
        for (FileInfo fileInfo : mFileList) {
            if (fileInfo.isSelected())
                res = FileHelper.moveFile(fileInfo.getPath(), FileLoader.CameraPath);
            else
                res = FileHelper.deleteFile(fileInfo.getPath());

            if (!res)
                Logger.getLogger().e( "move file " + fileInfo.getPath() + " to " + FileLoader.CameraPath + "  error!!!");
        }
    }

    private void saveAll() {
        for (FileInfo fileInfo : mFileList) {
            Boolean res = FileHelper.moveFile(fileInfo.getPath(), FileLoader.CameraPath);
            if (!res)
                Logger.getLogger().e( "move file " + fileInfo.getPath() + " to " + FileLoader.CameraPath + "  error!!!");
        }
    }

    @Override
    public Loader<List<FileInfo>> onCreateLoader(int id, Bundle args) {
        return new FileLoader(this, mPath);
    }

    @Override
    public void onLoadFinished(Loader<List<FileInfo>> loader, List<FileInfo> data) {
        mFileList = data;
        mAdapter.setData((ArrayList) mFileList);
    }

    @Override
    public void onLoaderReset(Loader<List<FileInfo>> loader) {
        mAdapter.setData(null);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mCurrentGalleryPosition = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class MySimpleGesture extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            updateCounter();
            return true;
        }
    }

    public void updateCounter() {
        FileInfo fileInfo = mFileList.get(mCurrentGalleryPosition);
        Boolean isSelected = !fileInfo.isSelected();
        fileInfo.setIsSelected(isSelected);

        if (isSelected)
            mSelectedNum++;
        else
            mSelectedNum--;

        mStatistics.setText(String.format(getResources().getString(R.string.select_items), mSelectedNum));
        //to notify adapter to update item view
        mAdapter.setData((ArrayList) mFileList);
    }
}

