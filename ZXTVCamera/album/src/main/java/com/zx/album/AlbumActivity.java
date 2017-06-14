/*
 * Copyright © Yan Zhenjie. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zx.album;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.yanzhenjie.fragment.CompatActivity;
import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.mediascanner.MediaScanner;
import com.zx.album.fragment.AlbumFragment;
import com.zx.album.fragment.CameraFragment;
import com.zx.album.fragment.GalleryFragment;
import com.zx.album.fragment.GelleryMenuDailog;
import com.zx.album.impl.AlbumCallback;
import com.zx.album.impl.CameraCallback;
import com.zx.album.impl.GalleryCallback;
import com.zx.album.util.AlbumUtils;
import com.zx.album.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.zx.album.BasicWrapper.VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM;
import static com.zx.album.BasicWrapper.VALUE_INPUT_FRAMEWORK_FUNCTION_GALLERY;

/**
 * <p>Responsible for controlling the album data and the overall logic.</p>
 * Created by Yan Zhenjie on 2016/10/17.
 * Modify by ShaudXioa for ZhaoXin TV on 2017/06/12
 */
public class AlbumActivity extends CompatActivity implements AlbumCallback, GalleryCallback, CameraCallback
        , GelleryMenuDailog.OnDialogListener {

    private static final int PERMISSION_REQUEST_STORAGE_ALBUM = 200;
    private static final int PERMISSION_REQUEST_STORAGE_GALLERY = 201;

    private List<String> mCheckedPaths;
    private String mCurrentImagepath;
    private Bundle mArgument;
    private int function;

    private GelleryMenuDailog mGelleryMenuDailog;
    private NoFragment mCurrentFragment;

    @Override
    protected int fragmentLayoutId() {
        return R.id.album_root_frame_layout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, flag);
        Log.e("DEBUG", "****************AlbumActivity");
        DisplayUtils.initScreen(this);
        // Language.
        Locale locale = Album.getAlbumConfig().getLocale();
        AlbumUtils.applyLanguageForContext(this, locale);

        setContentView(R.layout.album_activity_main);

        // prepare color.
        Intent intent = getIntent();
        mArgument = intent.getExtras();

        // basic.
        int statusBarColor = intent.getIntExtra(UIWrapper.KEY_INPUT_STATUS_COLOR,
                ContextCompat.getColor(this, R.color.album_ColorPrimaryDark));
        int navigationBarColor = intent.getIntExtra(UIWrapper.KEY_INPUT_NAVIGATION_COLOR,
                ContextCompat.getColor(this, R.color.album_ColorPrimaryBlack));
        mCheckedPaths = intent.getStringArrayListExtra(UIWrapper.KEY_INPUT_CHECKED_LIST);
        mCurrentImagepath = intent.getStringExtra(UIWrapper.KEY_INPUT_CHECKED_LIST_PATH);

        setWindowBarColor(statusBarColor, navigationBarColor);

        // Function dispatch.
        final int function = intent.getIntExtra(BasicWrapper.KEY_INPUT_FRAMEWORK_FUNCTION,
                VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM);
        gotoUI(function);
    }

    public void gotoUI(int function) {
        Log.d("debug", "*********** function: " + function);

        switch (function) {
            case VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM: {
                requestPermission(PERMISSION_REQUEST_STORAGE_ALBUM);
                break;
            }

            case BasicWrapper.VALUE_INPUT_FRAMEWORK_FUNCTION_GALLERY: {
//                if (mCheckedPaths == null || mCheckedPaths.size() == 0) {
//                    finish();
//                } else {
                requestPermission(PERMISSION_REQUEST_STORAGE_GALLERY);
//                }
                break;
            }
            case BasicWrapper.VALUE_INPUT_FRAMEWORK_FUNCTION_CAMERA: {

                CameraFragment fragment = NoFragment.instantiate(this, CameraFragment.class, mArgument);
                startFragment(fragment);
                break;
            }
            default: {
                finish();
                break;
            }
        }
    }

    /**
     * Set window bar color.
     *
     * @param statusColor     status bar color.
     * @param navigationColor navigation bar color.
     */
    private void setWindowBarColor(@ColorInt int statusColor, @ColorInt int navigationColor) {
        if (Build.VERSION.SDK_INT >= 21) {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(statusColor);
            window.setNavigationBarColor(navigationColor);
        }
    }

    /**
     * Scan, but unknown permissions.
     *
     * @param requestCode request code.
     */
    private void requestPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                onRequestPermissionsResult(
                        requestCode,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        new int[]{PackageManager.PERMISSION_GRANTED});
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        requestCode);
            }
        } else {
            onRequestPermissionsResult(
                    requestCode,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE_ALBUM: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mArgument.putBoolean(AlbumWrapper.KEY_INPUT_ALLOW_CAMERA, false);
                    AlbumFragment albumFragment = NoFragment.instantiate(this, AlbumFragment.class, mArgument);
                    startFragment(albumFragment);
                    mCurrentFragment = albumFragment;
                    function = VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM;
                } else {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setTitle(R.string.album_dialog_permission_failed)
                            .setMessage(R.string.album_permission_storage_failed_hint)
                            .setPositiveButton(R.string.album_dialog_sure, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onAlbumCancel();
                                }
                            })
                            .show();
                    function = -1;
                }
                break;
            }
            case PERMISSION_REQUEST_STORAGE_GALLERY: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GalleryFragment galleryFragment = NoFragment.instantiate(this, GalleryFragment.class, mArgument);
                    galleryFragment.bindImagePaths(mCheckedPaths);
                    galleryFragment.bindImageFolderPath(mCurrentImagepath);
                    startFragment(galleryFragment);
                    mCurrentFragment = galleryFragment;
                    function = VALUE_INPUT_FRAMEWORK_FUNCTION_GALLERY;
                } else {
                    onAlbumResult(new ArrayList<>(mCheckedPaths));
                    function = -1;
                }
                break;
            }
        }
    }

    @Override
    public void onAlbumResult(ArrayList<String> imagePathList) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(Album.KEY_OUTPUT_IMAGE_PATH_LIST, imagePathList);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onAlbumCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onGalleryResult(ArrayList<String> imagePathList) {
        onAlbumResult(imagePathList);
    }

    @Override
    public void onGalleryCancel() {
        onAlbumCancel();
    }

    @Override
    public void onCameraResult(String imagePath) {
        // Add media library.
        new MediaScanner(this).scan(imagePath);
        ArrayList<String> pathList = new ArrayList<>();
        pathList.add(imagePath);
        onAlbumResult(pathList);
    }

    @Override
    public void onCameraCancel() {
        onAlbumCancel();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_INFO ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            if (function == VALUE_INPUT_FRAMEWORK_FUNCTION_GALLERY) {
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
            } else if(function == VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM) {
                if(mCurrentFragment instanceof AlbumFragment) {
                    ((AlbumFragment)mCurrentFragment).showMenuDialog();
                }
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDialogClick(View view) {
        if(view.getId() == R.id.gotoGallery) {
            gotoUI(VALUE_INPUT_FRAMEWORK_FUNCTION_ALBUM);
        } else if(view.getId() == R.id.pic_delete) {

        }
    }
}