package com.zx.album.tv;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.zx.album.Album;
import com.zx.album.R;
import com.zx.album.adapter.BasicPreviewAdapter;
import com.zx.album.adapter.PathPreviewAdapter;
import com.zx.album.entity.AlbumImage;
import com.zx.album.task.FileLoader;

import java.util.ArrayList;
import java.util.List;


/**
 * User: ShaudXiao
 * Date: 2017-06-14
 * Time: 16:09
 * Company: zx
 * Description:
 * FIXME
 */


public class GalleryActivityTv extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<AlbumImage>>
        , GelleryMenuDailog.OnDialogListener {
    public static final String KEY_INPUT_CHECKED_LIST_PATH = "KEY_INPUT_CHECKED_LIST_PATH";
    public static final String KEY_INPUT_SELECT_INDEX = "KEY_INPUT_SELECT_INDEX";

    private View mCheckParent;

    private int mCurrentItemPosition = 0;
    private ViewPager mViewPager;
    private BasicPreviewAdapter previewAdapter;

    private List<String> mCheckedPaths = new ArrayList<>();
    private boolean[] mCheckedList;

    private String mCurrentImageFolderpath;
    private FileLoader mPicLoader;

    private TextView tvFileCounter;

    private GelleryMenuDailog mGelleryMenuDailog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        init();
    }

    private void init() {

        mCheckParent = findViewById(R.id.layout_gallery_preview_bottom);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        tvFileCounter = (TextView) findViewById(R.id.counter);

        mCurrentItemPosition = 0;

        Intent intent = getIntent();
        Bundle argument = intent.getExtras();
        if (argument == null) {
            finish();
        }



        String filePath = argument.getString(KEY_INPUT_CHECKED_LIST_PATH);
        mCurrentItemPosition = argument.getInt(KEY_INPUT_SELECT_INDEX);
        bindImageFolderPath(filePath);
        initializeViewPager();

        getLoaderManager().initLoader(0, null, this);

    }


    @Override
    public void onResume() {
        if (null != mPicLoader) {
            mPicLoader.forceLoad();
        }

        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_INFO ||
                keyCode == KeyEvent.KEYCODE_MENU) {

            FragmentManager fm = getFragmentManager();
            mGelleryMenuDailog = (GelleryMenuDailog) fm.findFragmentByTag("menu_dailog");
            if (mGelleryMenuDailog == null) {
                mGelleryMenuDailog = new GelleryMenuDailog();
                mGelleryMenuDailog.addOnDialogListener(this);
                if (!mGelleryMenuDailog.isVisible()) {
                    mGelleryMenuDailog.show(fm, "menu_dailog");
                } else {
                    mGelleryMenuDailog.dismiss();
                }
            } else {
                if (!mGelleryMenuDailog.isVisible()) {
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.show(mGelleryMenuDailog);
                } else {
                    mGelleryMenuDailog.dismiss();
                }
            }


            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDialogClick(View view) {
        if (view.getId() == R.id.gotoGallery) {
            Album.albumTV(this).start();
        } else if (view.getId() == R.id.pic_delete) {
            deleteItem();
        }
    }

    private void deleteItem() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((PathPreviewAdapter)previewAdapter).deleteItem(mCurrentItemPosition);

                        updateCounter(mCurrentItemPosition);
                    }
                })
                .setNegativeButton(R.string.cancle, null)
                .show();
    }

    public void updateCounter(int pos) {
        int rank;
        if ((pos + 1) <= previewAdapter.getCount())
            rank = pos + 1;
        else
            rank = previewAdapter.getCount();
        String s = String.format("%d/%d", rank, previewAdapter.getCount());
        tvFileCounter.setText(s);
        mCurrentItemPosition = pos;
        // after delete the last file, just turn back to camera
        if (previewAdapter.getCount() == 0)
            this.finish();
    }

    /**
     * Bind the preview picture collection.
     *
     * @param imagePaths image list of local.
     */
    public void bindImagePaths(List<String> imagePaths) {
        if (imagePaths == null) {
            return;
        }
        mCheckedPaths.clear();
        mCheckedPaths.addAll(imagePaths);
        int length = mCheckedPaths.size();
        mCheckedList = new boolean[length];
        for (int i = 0; i < length; i++) {
            mCheckedList[i] = true;
        }
    }

    public void bindImageFolderPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            mCurrentImageFolderpath = path;
        } else {
            mCurrentImageFolderpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
                    + "Camera";

        }

    }

    private void initializeViewPager() {
        if (mCheckedPaths.size() > 2)
            mViewPager.setOffscreenPageLimit(2);

        previewAdapter = new PathPreviewAdapter(mCheckedPaths);
        mViewPager.setAdapter(previewAdapter);
        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentItemPosition = position;

                tvFileCounter.setText(mCurrentItemPosition + 1 + " / " + mCheckedPaths.size());
            }
        };
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setCurrentItem(mCurrentItemPosition);
        // Forced call.
//        pageChangeListener.onPageSelected(mCurrentItemPosition);
    }


    @Override
    public Loader<List<AlbumImage>> onCreateLoader(int id, Bundle args) {
        mPicLoader = new FileLoader(this, mCurrentImageFolderpath);
        return mPicLoader;
    }

    @Override
    public void onLoadFinished(android.content.Loader<List<AlbumImage>> loader, List<AlbumImage> data) {
        mCheckedPaths.clear();
        Log.d("debug", "************** size " + data.size());
        ArrayList<String> list = new ArrayList<>();
        for (AlbumImage info : data) {

            list.add(info.getPath());
        }
        bindImagePaths(list);
        tvFileCounter.setText(mCurrentItemPosition + 1 + " / " + mCheckedPaths.size());
        previewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(android.content.Loader<List<AlbumImage>> loader) {

    }

}
