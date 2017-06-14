package com.zx.tv.camera.gallery;


import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.zx.tv.camera.R;
import com.zx.tv.camera.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryMainActivity extends AppCompatActivity implements
        Gallery.OnItemSelectedListener,
        LoaderManager.LoaderCallbacks<List<FileInfo>>
        , MenuDailog.OnDialogListener {

    private static final String TAG = "com.zhaoxin.zxcamera.gallery.GalleryMainActivity";
    private PicGallery mGallery;
    private TextView mStatistics;
    private int mCurrentGalleryPosition = 0;
    private GalleryAdapter mAdapter;
    private String mDetailInfo = "sorry to get the detail info";
    private Boolean mShowButtons = true;
    private FileLoader mPicLoader;
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private MenuDailog mMenuDailog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_view);

        init();

        getLoaderManager().initLoader(0, null, this);

        initSize();

        initImageLoader();
    }

    private void init() {

        mStatistics = (TextView) findViewById(R.id.counter);

        mGallery = (PicGallery) findViewById(R.id.pic_gallery);
        mGallery.setVerticalFadingEdgeEnabled(false);
        mGallery.setHorizontalFadingEdgeEnabled(false);
        mGallery.setDetector(new GestureDetector(this, new MySimpleGesture()));
        mGallery.setOnItemSelectedListener(this);
        mAdapter = new GalleryAdapter(this);

        mGallery.setAdapter(mAdapter);
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

    public void onResume() {

        if (mPicLoader != null)
            mPicLoader.forceLoad();

        super.onResume();
    }

    private final String GALLERY_PACKAGE_NAME = "com.android.gallery3d";
    private final String GALLERY_ACTIVITY_CLASS = "com.android.gallery3d.app.GalleryActivity";

    @Override
    public void onDialogClick(View view) {
        switch (view.getId()) {
            case R.id.gotoGallery:
                gotoGallery();
                break;

            case R.id.pic_delete:
                deleteItem();
                break;
            default:
                break;
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU ||
                keyCode == KeyEvent.KEYCODE_INFO) {

            FragmentManager fm = getFragmentManager();
            mMenuDailog = (MenuDailog)fm.findFragmentByTag("menu_dailog");
            if (mMenuDailog == null) {
                mMenuDailog = new MenuDailog();
                mMenuDailog.addOnDialogListener(this);
                if (!mMenuDailog.isVisible()) {
                    mMenuDailog.setFileName(((FileInfo)mAdapter.getItem(mCurrentGalleryPosition)).getName());
                    mMenuDailog.show(fm, "menu_dailog");
                } else {
                    mMenuDailog.dismiss();
                }
            } else {
                if (!mMenuDailog.isVisible()) {
                    FragmentTransaction ft = fm.beginTransaction();
                    mMenuDailog.setFileName(((FileInfo)mAdapter.getItem(mCurrentGalleryPosition)).getName());
                    ft.show(mMenuDailog);
                } else {
                    mMenuDailog.dismiss();
                }
            }
            return true;

        }
        return super.onKeyDown(keyCode, event);
    }

    private void gotoGallery() {
        Intent intent;
        intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(GALLERY_PACKAGE_NAME, GALLERY_ACTIVITY_CLASS);
        // check if intent can launch gallery
        PackageManager pm = this.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.size() == 0) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*|video/*");

            //another method to open video and picture
                    /*Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setType("video/*");
                    i.setType("image/*");*/

            startActivity(i);
            Logger.getLogger().d("start gallery from action_view");
        } else {
            startActivity(intent);
            Logger.getLogger().d("start gallery from package name");
        }
    }


    private void showDetail() {
        FileInfo curFileInfo;
        curFileInfo = (FileInfo) mAdapter.getItem(mCurrentGalleryPosition);
        mDetailInfo = "";
        if (curFileInfo.getMineType().equals("image/*")) {
            try {
                ExifInfo exif = new ExifInfo(curFileInfo.getPath());
                mDetailInfo = String.format("Tile:%s\nTime: %s\nFile width:%s\nFile height:%s\nOrientation:%s\nFile size:%s\nMaker:%s\nModel:%s\nFlash:%s\nFocal Legth:%s\nWhite balance:%s\nAperture:%s\nExposure time:%s\nISO:%s\nPath:%s",
                        exif.getTitle(), exif.getDateTime(), exif.getImageWidth(), exif.getImageHeight(), exif.getImageOrientation(), exif.getFileSize(),
                        exif.getMaker(), exif.getModel(), exif.getFlash(), exif.getFocalLength(), exif.getWhiteBalance(), exif.getAperture(), exif.getExposureTime(),
                        exif.getIso(), exif.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (curFileInfo.getMineType().equals("video/*")) {
            VideoMetadata metadata = new VideoMetadata(curFileInfo.getPath());
            mDetailInfo = String.format("Tile:%s\nTime:%s\nWidth:%s\nHeight:%s\nDuration:%s\nFile Size:%s\nPath:%s",
                    metadata.getTitle(), metadata.getTime(), metadata.getWidth(), metadata.getHeight(), metadata.getDuration(), metadata.getFileSize(), metadata.getPath());

        }

        new AlertDialog.Builder(this)
                .setTitle("Details")
                .setNegativeButton("Close", null)
                .setMessage(mDetailInfo)
                .show();
    }

    public void shareItem() {
        Intent intent;
        FileInfo curFileInfo;
        intent = new Intent(Intent.ACTION_SEND);
        curFileInfo = (FileInfo) mAdapter.getItem(mCurrentGalleryPosition);
        File f = new File(curFileInfo.getPath());
        if (f != null && f.exists() && f.isFile()) {
            intent.setType(curFileInfo.getMineType());
            Uri u = Uri.fromFile(f);
            intent.putExtra(Intent.EXTRA_STREAM, u);
        }
        startActivity(intent);
    }

    private void deleteItem() {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("this picture or video will be deleted !")
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAdapter.deleteItem(mCurrentGalleryPosition);
                        updateCounter(mCurrentGalleryPosition);
                    }
                })
                .setNegativeButton(R.string.cancle, null)
                .show();
    }

    public void updateCounter(int pos) {
        int rank;
        if ((pos + 1) <= mAdapter.getCount())
            rank = pos + 1;
        else
            rank = mAdapter.getCount();
        String s = String.format("%d/%d", rank, mAdapter.getCount());
        mStatistics.setText(s);
        mCurrentGalleryPosition = pos;
        // after delete the last file, just turn back to camera
        if (mAdapter.getCount() == 0)
            this.finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateCounter(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class MySimpleGesture extends GestureDetector.SimpleOnGestureListener {
        //the second touch down trigger action
        public boolean onDoubleTap(MotionEvent e) {
            View view = mGallery.getSelectedView().findViewById(R.id.bitmap);
            if (view instanceof GalleryImageView) {
                GalleryImageView imageView = (GalleryImageView) view;
                if (imageView.getScale() > imageView.getMiniZoom()) {
                    imageView.zoomTo(imageView.getMiniZoom());
                } else {
                    imageView.zoomTo(imageView.getMaxZoom());
                }

            } else {

            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            mShowButtons = !mShowButtons;
            showButtons(mShowButtons);
            return true;
        }
    }

    private void showButtons(Boolean show) {
        int visual;
        if (show)
            visual = View.VISIBLE;
        else
            visual = View.INVISIBLE;


        mStatistics.setVisibility(visual);

    }

    @Override
    public Loader<List<FileInfo>> onCreateLoader(int id, Bundle args) {
        mPicLoader = new FileLoader(this);
        return mPicLoader;
    }


    @Override
    public void onLoadFinished(Loader<List<FileInfo>> loader, List<FileInfo> data) {
        mAdapter.setData((ArrayList) data);
    }

    @Override
    public void onLoaderReset(Loader<List<FileInfo>> loader) {
        mAdapter.setData(null);
    }

}
