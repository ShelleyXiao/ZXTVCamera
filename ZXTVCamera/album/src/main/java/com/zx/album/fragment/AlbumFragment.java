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
package com.zx.album.fragment;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.yanzhenjie.fragment.NoFragment;
import com.yanzhenjie.mediascanner.MediaScanner;
import com.zx.album.AlbumWrapper;
import com.zx.album.R;
import com.zx.album.UIWrapper;
import com.zx.album.adapter.AlbumImagebyCvAdapter;
import com.zx.album.dialog.AlbumFolderDialog;
import com.zx.album.entity.AlbumFolder;
import com.zx.album.entity.AlbumImage;
import com.zx.album.impl.AlbumCallback;
import com.zx.album.impl.OnCompatItemClickListener;
import com.zx.album.impl.OnCompoundItemCheckListener;
import com.zx.album.task.ScanTask;
import com.zx.album.widget.recyclerview.TvRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>Image list fragment.</p>
 * Created by Yan Zhenjie on 2017/3/25.
 */
public class AlbumFragment extends BasicCameraFragment {

    private static final int REQUEST_CODE_FRAGMENT_PREVIEW = 100;
    private static final int REQUEST_CODE_FRAGMENT_NULL = 101;

    private MediaScanner mMediaScanner;

    private AlbumCallback mCallback;

    private int mToolBarColor;
    private int mNavigationColor;

    private Button mBtnPreview;
    private Button mBtnSwitchFolder;

    private boolean mHasCamera;

    private TvRecyclerView mRvContentList;
    private GridLayoutManager mLayoutManager;

    private LinearLayoutManager mLinearLayoutManager;

    private AlbumImagebyCvAdapter mAlbumContentAdapter;

    private List<AlbumFolder> mAlbumFolders;
    private List<AlbumImage> mCheckedImages = new ArrayList<>(1);
    private int mCurrentFolderPosition;

    private int mAllowSelectCount;

    private AlbumFolderDialog mAlbumFolderDialog;
    private AlbumMenuDailog mAlbumMenuDailog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (AlbumCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mCallback = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.album_fragment_album, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mBtnPreview = (Button) view.findViewById(R.id.btn_preview);
        mBtnSwitchFolder = (Button) view.findViewById(R.id.btn_switch_dir);
        mRvContentList = (TvRecyclerView) view.findViewById(R.id.rv_content_list);

//        setToolbar((Toolbar) view.findViewById(R.id.toolbar));
//        displayHomeAsUpEnabled(R.drawable.album_ic_back_white);

        mBtnSwitchFolder.setOnClickListener(mSwitchDirClick);
        mBtnPreview.setOnClickListener(mPreviewClick);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Bundle argument = getArguments();
//        mToolBarColor = argument.getInt(
//                AlbumWrapper.KEY_INPUT_TOOLBAR_COLOR,
//                ContextCompat.getColor(getContext(), R.color.album_ColorPrimary));
//        String title = argument.getString(AlbumWrapper.KEY_INPUT_TITLE);
//        if (TextUtils.isEmpty(title)) title = getString(R.string.album_title);
//        mNavigationColor = argument.getInt(
//                AlbumWrapper.KEY_INPUT_NAVIGATION_COLOR,
//                ContextCompat.getColor(getContext(), R.color.album_ColorPrimaryBlack));
        int columnCount = argument.getInt(AlbumWrapper.KEY_INPUT_COLUMN_COUNT, 2);
        mAllowSelectCount = argument.getInt(AlbumWrapper.KEY_INPUT_LIMIT_COUNT, Integer.MAX_VALUE);
        mHasCamera = argument.getBoolean(AlbumWrapper.KEY_INPUT_ALLOW_CAMERA, true);

        // noinspection ConstantConditions
//        getToolbar().setBackgroundColor(mToolBarColor);
//        setTitle(title);

//        mLayoutManager = new GridLayoutManager(getContext(), columnCount);
//        mRvContentList.setLayoutManager(mLayoutManager);


        GridLayoutManager manager = new GridLayoutManager(getActivity(), 1);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        manager.supportsPredictiveItemAnimations();
        mRvContentList.setLayoutManager(manager);

        int itemSpace = getResources().
                getDimensionPixelSize(R.dimen.album_dp_6);
        mRvContentList.addItemDecoration(new SpaceItemDecoration(itemSpace));
        DefaultItemAnimator animator = new DefaultItemAnimator();
        mRvContentList.setItemAnimator(animator);
        mRvContentList.setSelectedScale(1.08f);

        mRvContentList.requestFocus();

        mRvContentList.getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override
            public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                Log.i("debug", "new Focus " + newFocus + " *** old Focus: " + oldFocus);
                if(newFocus instanceof RecyclerView) {
//                    if(mRvContentList.getChildCount() > 0) {
//                        mRvContentList.getChildAt(0).requestFocus();
//                    }
                }
            }
        });

        mRvContentList.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    if(mRvContentList.getChildCount() > 0) {
                        mRvContentList.getChildAt(0).requestFocus();
                    }
                }
            }
        });

//        mLinearLayoutManager = new LinearLayoutManager(getContext());
//        mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        mRvContentList.setLayoutManager(mLinearLayoutManager);


//        Drawable decoration = ContextCompat.getDrawable(getContext(), R.drawable.album_decoration_white);
//        mRvContentList.addItemDecoration(new AlbumVerticalGirdDecoration(decoration));

//        int itemSize = (DisplayUtils.screenWidth - decoration.getIntrinsicWidth() * (columnCount + 1)) / columnCount;
        mAlbumContentAdapter = new AlbumImagebyCvAdapter(getContext());
        mAlbumContentAdapter.setAddPhotoClickListener(mAddPhotoListener);
//        mAlbumContentAdapter.setOnCheckListener(mItemCheckListener);
        mAlbumContentAdapter.setItemClickListener(new OnCompatItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                List<AlbumImage> albumImages = mAlbumFolders.get(mCurrentFolderPosition).getImages();
                AlbumPreviewFragment previewFragment = NoFragment.instantiate(getContext(), AlbumPreviewFragment.class, argument);
                previewFragment.bindAlbumImages(albumImages, mCheckedImages, position);
                startFragmentForResquest(previewFragment, REQUEST_CODE_FRAGMENT_PREVIEW);
            }
        });
        mRvContentList.setAdapter(mAlbumContentAdapter);

        ScanTask scanTask = new ScanTask(getContext(), mScanCallback, mCheckedImages);
        List<String> checkedPathList = argument.getStringArrayList(UIWrapper.KEY_INPUT_CHECKED_LIST);
        //noinspection unchecked
        scanTask.execute(checkedPathList);
    }

    /**
     * Scan the picture result callback.
     */
    private ScanTask.Callback mScanCallback = new ScanTask.Callback() {
        @Override
        public void onScanCallback(List<AlbumFolder> folders) {
            mAlbumFolders = folders;
            if (mAlbumFolders.get(0).getImages().size() == 0) {
                AlbumNullFragment nullFragment = NoFragment.instantiate(getContext(), AlbumNullFragment.class, getArguments());
                startFragmentForResquest(nullFragment, REQUEST_CODE_FRAGMENT_NULL);
            } else showImageFromFolder(0);
        }
    };

    /**
     * Update data source.
     */
    private void showImageFromFolder(int position) {
        AlbumFolder albumFolder = mAlbumFolders.get(position);
        mBtnSwitchFolder.setText(albumFolder.getName());
        mAlbumContentAdapter.notifyDataSetChanged(albumFolder.getImages());
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Bundle result) {
        switch (requestCode) {
            case REQUEST_CODE_FRAGMENT_PREVIEW: { // Preview finish, to refresh check status.
                showImageFromFolder(mCurrentFolderPosition);
                setCheckedCountUI(mCheckedImages.size());

                if (resultCode == RESULT_OK) {
                    onAlbumResult();
                }
                break;
            }
            case REQUEST_CODE_FRAGMENT_NULL: {
                if (resultCode == RESULT_OK) {
                    String imagePath = AlbumNullFragment.parseImagePath(result);
                    onCameraBack(imagePath);
                } else {
                    mCallback.onAlbumCancel();
                }
                break;
            }
        }
    }

    /**
     * Switch folder.
     */
    private View.OnClickListener mSwitchDirClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mAlbumFolderDialog == null)
                mAlbumFolderDialog = new AlbumFolderDialog(
                        getContext(),
                        mToolBarColor,
                        mNavigationColor,
                        mAlbumFolders,
                        new OnCompatItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                if (mAlbumFolders.size() > position) {
                                    mCurrentFolderPosition = position;
                                    showImageFromFolder(mCurrentFolderPosition);
                                    mLayoutManager.scrollToPosition(0);
                                }
                            }
                        });
            if (!mAlbumFolderDialog.isShowing())
                mAlbumFolderDialog.show();

            if (mAlbumFolderDialog == null)
                mAlbumFolderDialog = new AlbumFolderDialog(
                        getContext(),
                        mToolBarColor,
                        mNavigationColor,
                        mAlbumFolders,
                        new OnCompatItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                if (mAlbumFolders.size() > position) {
                                    mCurrentFolderPosition = position;
                                    showImageFromFolder(mCurrentFolderPosition);
                                    mLayoutManager.scrollToPosition(0);
                                }
                            }
                        });
            if (!mAlbumFolderDialog.isShowing())
                mAlbumFolderDialog.show();
        }
    };

    public void showMenuDialog() {
        if (mAlbumMenuDailog == null)
            mAlbumMenuDailog = new AlbumMenuDailog(
                    getContext(),
                    mAlbumFolders,
                    new OnCompatItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            if (mAlbumFolders.size() > position) {
                                mCurrentFolderPosition = position;
                                showImageFromFolder(mCurrentFolderPosition);
//                                mLayoutManager.scrollToPosition(0);
                            }
                        }
                    });
        if (!mAlbumMenuDailog.isShowing())
            mAlbumMenuDailog.show();
    }

    /**
     * Camera.
     */
    private OnCompatItemClickListener mAddPhotoListener = new OnCompatItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            int hasCheckSize = mCheckedImages.size();
            if (hasCheckSize >= mAllowSelectCount)
                Toast.makeText(
                        getContext(),
                        String.format(Locale.getDefault(), getString(R.string.album_check_limit_camera), mAllowSelectCount),
                        Toast.LENGTH_LONG).show();
            else
                cameraUnKnowPermission(randomJPGPath());
        }
    };

    @Override
    protected void onCameraBack(String imagePath) {
        // Add media lib.
        if (mMediaScanner == null) {
            mMediaScanner = new MediaScanner(getContext());
        }
        mMediaScanner.scan(imagePath);

        String name = new File(imagePath).getName();
        AlbumImage image = new AlbumImage(0, imagePath, name, SystemClock.elapsedRealtime());
        image.setChecked(true);

        mCheckedImages.add(image);
        setCheckedCountUI(mCheckedImages.size());

        List<AlbumImage> images = mAlbumFolders.get(0).getImages();
        if (images.size() > 0) {
            images.add(0, image);

            if (mCurrentFolderPosition == 0) {
                mAlbumContentAdapter.notifyItemInserted(mHasCamera ? 1 : 0);
            } else showImageFromFolder(0);
        } else {
            images.add(image);
            showImageFromFolder(0);
        }
    }

    /**
     * When a picture is selected.
     */
    private OnCompoundItemCheckListener mItemCheckListener = new OnCompoundItemCheckListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, int position, boolean isChecked) {
            AlbumImage albumImage = mAlbumFolders.get(mCurrentFolderPosition).getImages().get(position);
            albumImage.setChecked(isChecked);

            if (isChecked) {
                if (mCheckedImages.size() >= mAllowSelectCount) {
                    Toast.makeText(
                            getContext(),
                            String.format(Locale.getDefault(), getString(R.string.album_check_limit), mAllowSelectCount),
                            Toast.LENGTH_LONG).show();

                    buttonView.setChecked(false);
                    albumImage.setChecked(false);
                } else {
                    mCheckedImages.add(albumImage);
                }
            } else {
                mCheckedImages.remove(albumImage);
            }
            setCheckedCountUI(mCheckedImages.size());
        }
    };

    /**
     * Set the number of selected pictures.
     *
     * @param count number.
     */
    public void setCheckedCountUI(int count) {
        mBtnPreview.setText(" (" + count + ")");
        //noinspection ConstantConditions
//        getToolbar().setSubtitle(count + "/" + mAllowSelectCount);
    }

    /**
     * The preview button is clicked.
     */
    private View.OnClickListener mPreviewClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCheckedImages.size() > 0) {
                AlbumPreviewFragment previewFragment = NoFragment.instantiate(getContext(),
                        AlbumPreviewFragment.class,
                        getArguments());
                previewFragment.bindAlbumImages(mCheckedImages, mCheckedImages, 0);
                startFragmentForResquest(previewFragment, REQUEST_CODE_FRAGMENT_PREVIEW);
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.album_menu_album, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            mCallback.onAlbumCancel();
        } else if (itemId == R.id.album_menu_finish) {
            onAlbumResult();
        }
        return true;
    }

    /**
     * Photo album callback selection result.
     */
    private void onAlbumResult() {
        int allSize = mAlbumFolders.get(0).getImages().size();
        int checkSize = mCheckedImages.size();
        if (allSize > 0 && checkSize == 0) {
            Toast.makeText(getContext(), R.string.album_check_little, Toast.LENGTH_LONG).show();
        } else if (checkSize == 0) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            setResult(RESULT_OK);
            ArrayList<String> pathList = new ArrayList<>();
            for (AlbumImage albumImage : mCheckedImages) {
                pathList.add(albumImage.getPath());
            }
            mCallback.onAlbumResult(pathList);
        }
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.left = space;
        }
    }

}
