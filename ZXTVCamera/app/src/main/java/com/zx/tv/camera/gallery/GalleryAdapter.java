package com.zx.tv.camera.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.zx.tv.camera.R;
import com.zx.tv.camera.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class GalleryAdapter extends BaseAdapter {

    private Context context;
    private static final String TAG = "com.zhaoxin.zxcamera.gallery.GalleryAdapter";

    private ArrayList<FileInfo> mItems;

    private LayoutInflater mInflator;

    private FileInfo mCurrentItem;

    private ImageLoader mImageLoader;

    private DisplayImageOptions options;

    private Boolean mShowSelectedView = false;

    int mCurrentPosition = 0;


    public void setData(ArrayList<FileInfo> data) {
        this.mItems = data;
        notifyDataSetChanged();
    }

    public GalleryAdapter(Context context) {
        this.context = context;
        mInflator = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        options = new DisplayImageOptions.Builder()
//                .showStubImage(R.drawable.ic_launcher)
//                .showImageForEmptyUri(R.drawable.ic_launcher)
//                .showImageOnFail(R.drawable.ic_launcher)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    public Boolean deleteItem(int position) {
        Boolean res = false;

        if (mItems == null) {
            Logger.getLogger().e(  "deleteItem error, mItems is null!!!");
            return res;
        }

        if (position < 0 || position >= mItems.size()) {
            Logger.getLogger().e(  "deleteItem error, position is out of bounds!!!");
            return res;
        }

        FileInfo item = mItems.get(position);
        if (item.isContinuePics()) {
            res = FileHelper.deleteFiles(item.getParentFolderPath());
        } else {
            res = FileHelper.deleteFile(item.getPath());
        }
        mItems.remove(position);
        notifyDataSetChanged();
        return res;
    }

    @Override
    public Object getItem(int position) {
        if (mItems == null) {
            Logger.getLogger().e( "getItem error, mItems is null!!!");
            return null;
        }
        if (position < 0 || position >= mItems.size()) {
            Logger.getLogger().e(  "getItem error, position is out of bounds!!!");
            return null;
        }

        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView;
        if (position < 0 || position >= mItems.size()) {
            Logger.getLogger().e(  "getView error, position is out of bounds!!!");
            return null;
        }

        if (convertView == null)
            itemView = mInflator.inflate(R.layout.image_item, null);
        else
            itemView = convertView;

        GalleryImageView imageView = (GalleryImageView) itemView.findViewById(R.id.bitmap);
        final FileInfo item = mItems.get(position);

        if (item == null) {
            Logger.getLogger().e(  "get view error, item is null!!!");
        }
        String uri = "file://" + item.getPath();
        mImageLoader.displayImage(uri, imageView, options);
        mCurrentItem = item;
        mCurrentPosition = position;

        if (item.getMineType().equals("video/*")) {
            ImageButton mPlayBtn = (ImageButton) itemView.findViewById(R.id.playButton);
            mPlayBtn.setVisibility(View.VISIBLE);
            mPlayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {//use system player to play video
                    Uri uri = Uri.fromFile(new File(mCurrentItem.getPath()));
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");
                    context.startActivity(intent);
                }
            });
        }

        //check whether picture is HDR
        if (item.getMineType().equals("image/*")) {
            try {
                ExifInfo exif = new ExifInfo(item.getPath());
                ImageView HDRView = (ImageView) itemView.findViewById(R.id.HDR_indication);
                Boolean HDR = false;
                // TODO: if get HDR info set HDR indicator visible
                if (HDR == true) {
                    HDRView.setVisibility(View.VISIBLE);
                } else {
                    HDRView.setVisibility(View.INVISIBLE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //check whether picture is continue taken pictures
        if (item.isContinuePics() == true) {
            Button ContinueView = (Button) itemView.findViewById(R.id.Continue_indication);
            ContinueView.setVisibility(View.VISIBLE);
            ContinueView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ContinuePicsActivity.class);
                    intent.putExtra("PATH", item.getParentFolderPath());
                    context.startActivity(intent);
                }
            });
        }

        if (mShowSelectedView) {
            ImageView selectedView = (ImageView) itemView.findViewById(R.id.select_indicator);
            selectedView.setVisibility(View.VISIBLE);
            if (item.isSelected())
                selectedView.setImageResource(R.drawable.tick);

        }

        return itemView;
    }

    public void setImageLoader(ImageLoader loader) {
        mImageLoader = loader;
    }

    public void showSelectedView(boolean show) {
        mShowSelectedView = show;
    }
}

