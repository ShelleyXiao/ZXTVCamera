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
package com.zx.album.tv;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.zx.album.Album;
import com.zx.album.R;
import com.zx.album.entity.AlbumImage;
import com.zx.album.impl.OnCompatItemClickListener;
import com.zx.album.impl.OnCompoundItemCheckListener;

import java.util.List;

/**
 * <p>Picture list display adapter.</p>
 * Created by Yan Zhenjie on 2016/10/18.
 */
public class AlbumImagebyCvAdapter extends RecyclerView.Adapter<AlbumImagebyCvAdapter.ItemViewHolder> {

    private static final int TYPE_BUTTON = 1;
    private static final int TYPE_IMAGE = 2;

    private boolean hasCamera;

    private LayoutInflater mInflater;

    private List<AlbumImage> mAlbumImages;

    private OnCompatItemClickListener mAddPhotoClickListener;

    private OnCompatItemClickListener mItemClickListener;

    private OnCompoundItemCheckListener mOnCompatCheckListener;


    public AlbumImagebyCvAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public void notifyDataSetChanged(List<AlbumImage> albumImages) {
        this.mAlbumImages = albumImages;
        super.notifyDataSetChanged();
    }

    public void setAddPhotoClickListener(OnCompatItemClickListener addPhotoClickListener) {
        this.mAddPhotoClickListener = addPhotoClickListener;
    }

    public void setItemClickListener(OnCompatItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public int getItemCount() {
        return mAlbumImages == null ? 0 : mAlbumImages.size();
    }

//    @Override
//    public int getItemViewType(int position) {
//        switch (position) {
//            case 0:
//                return hasCamera ? TYPE_BUTTON : TYPE_IMAGE;
//            default:
//                return TYPE_IMAGE;
//        }
//    }

    public void setOnCheckListener(OnCompoundItemCheckListener checkListener) {
        this.mOnCompatCheckListener = checkListener;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemViewHolder viewHolder = new ImageHolder(
                mInflater.inflate(R.layout.album_item_content_image, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        int imagePosition = holder.getAdapterPosition();
        AlbumImage albumImage = mAlbumImages.get(imagePosition);

        ImageHolder imageHolder = (ImageHolder) holder;
        imageHolder.mItemClickListener = mItemClickListener;
        imageHolder.setData(albumImage);


    }

    private static class ImageHolder extends ItemViewHolder implements View.OnClickListener {

        private ImageView mIvImage;
        private AppCompatCheckBox mCbChecked;

        private OnCompoundItemCheckListener mOnCompatCheckListener;

        public ImageHolder(View itemView) {
            super(itemView);
            mIvImage = (ImageView) itemView.findViewById(R.id.iv_album_content_image);
//            mCbChecked = (AppCompatCheckBox) itemView.findViewById(R.id.cb_album_check);
//            mCbChecked.setOnClickListener(this);
        }


        public void setData(AlbumImage albumImage) {
//            mCbChecked.setChecked(albumImage.isChecked());
            Album.getAlbumConfig().getImageLoader().loadImage(mIvImage, albumImage.getPath(), 240, 160);
        }


        @Override
        public void onClick(View v) {
//            if (mOnCompatCheckListener != null && v == mCbChecked) {
//                boolean isChecked = mCbChecked.isChecked();
//                int camera = hasCamera ? 1 : 0;
//                mOnCompatCheckListener.onCheckedChanged(mCbChecked, getAdapterPosition() - camera, isChecked);
//            }
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        OnCompatItemClickListener mItemClickListener;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(mClickListener);
        }

        private final View.OnClickListener mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null && v == itemView) {
                    mItemClickListener.onItemClick(v, getAdapterPosition());
                }
            }
        };
    }
}
