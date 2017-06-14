package com.zx.album.fragment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.zx.album.R;
import com.zx.album.adapter.AlbumFolderAdapter;
import com.zx.album.entity.AlbumFolder;
import com.zx.album.impl.OnCompatItemClickListener;

import java.util.List;


/**
 * User: ShaudXiao
 * Date: 2017-06-12
 * Time: 15:20
 * Company: zx
 * Description:
 * FIXME
 */


public class AlbumMenuDailog extends AppCompatDialog {

    private int mCurrentPosition = 0;
    private OnCompatItemClickListener mItemClickListener;
    private List<AlbumFolder> mAlbumFolders;


    public AlbumMenuDailog(@NonNull Context context,
                             @Nullable List<AlbumFolder> albumFolders,
                             @Nullable OnCompatItemClickListener itemClickListener) {
        super(context, R.style.LeftTransparent);
        setContentView(R.layout.album_dialog_floder);

        setCanceledOnTouchOutside(true);

        mAlbumFolders = albumFolders;
        mItemClickListener = itemClickListener;

        Window window = getWindow();

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = 400;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.LEFT;
        window.setAttributes(lp);

        RecyclerView rvContentList = (RecyclerView) findViewById(R.id.rv_content_list);
        assert rvContentList != null;
        rvContentList.setLayoutManager(new LinearLayoutManager(getContext()));


        rvContentList.setAdapter(new AlbumFolderAdapter(mAlbumFolders, new OnCompatItemClickListener() {
            @Override
            public void onItemClick(final View view, final int position) {

                if (mItemClickListener != null && mCurrentPosition != position) {
                    mCurrentPosition = position;
                    mItemClickListener.onItemClick(view, position);
                }

                dismiss();
            }
        }));
    }


}
