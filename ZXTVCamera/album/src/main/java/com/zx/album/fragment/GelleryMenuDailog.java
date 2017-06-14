package com.zx.album.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.zx.album.R;


/**
 * User: ShaudXiao
 * Date: 2017-06-12
 * Time: 15:20
 * Company: zx
 * Description:
 * FIXME
 */


public class GelleryMenuDailog extends DialogFragment implements  View.OnClickListener {

    private final String GALLERY_PACKAGE_NAME = "com.android.gallery3d";
    private final String GALLERY_ACTIVITY_CLASS = "com.android.gallery3d.app.GalleryActivity";

    private String mFilename;
    private TextView tvFileName;
    private Button mGotoGallery;
    private Button mDelete;
    private OnDialogListener mOnDialogListener;

    public GelleryMenuDailog() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setStyle(0, R.style.DownTransparent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gallery_menu_layout, container, false);
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();

        WindowManager.LayoutParams lp =  window.getAttributes();
        lp.height = 120;
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);

        tvFileName = (TextView) view.findViewById(R.id.name);
        mGotoGallery = (Button) view.findViewById(R.id.gotoGallery);
        mGotoGallery.setOnClickListener(this);
        mDelete = (Button) view.findViewById(R.id.pic_delete);
        mDelete.setOnClickListener(this);

        tvFileName.setText(mFilename);

        return view;
    }

    @Override
    public void onClick(View v) {
       if(mOnDialogListener != null) {
           mOnDialogListener.onDialogClick(v);
       }
       dismiss();
    }

    public void addOnDialogListener(OnDialogListener listener) {
        this.mOnDialogListener = listener;
    }

    public void setFileName(String name) {
        mFilename = name;
    }


    public interface OnDialogListener {
        void onDialogClick(View view);
    }
}
