package com.zx.tv.camera;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.zx.tv.camera.utils.Logger;

public class MainActivity extends BaseActivity implements View.OnClickListener
        , CameraDialog.CameraDialogParent , View.OnKeyListener{

    private static final float[] BANDWIDTH_FACTORS = {0.5F, 0.5F};

    private USBMonitor mUSBMonitor;

    private RelativeLayout mLeftCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerL;
    private CameraViewInterface mUVCCameraViewR;
    private Surface mLeftPrSurface;

    private RelativeLayout mRightCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerR;
    private CameraViewInterface mUVCCameraViewL;
    private Surface mRightPrSurface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraViewL != null) {
            mUVCCameraViewL.onResume();
        }
        if (mUVCCameraViewR != null) {
            mUVCCameraViewR.onResume();
        }
    }

    @Override
    protected void onStop() {
        mUVCCameraHandlerL.close();
        if (mUVCCameraViewL != null) {
            mUVCCameraViewL.onPause();
        }

        mUVCCameraHandlerR.close();
        if (mUVCCameraViewR != null) {
            mUVCCameraViewR.onPause();
        }

        mUSBMonitor.unregister();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mUVCCameraHandlerL != null) {
            mUVCCameraHandlerL = null;
        }
        if (mUVCCameraHandlerR != null) {
            mUVCCameraHandlerR = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraViewL = null;
        mUVCCameraViewR = null;

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.camera_layout_L) {
            if (mUVCCameraHandlerL != null) {
                if (!mUVCCameraHandlerL.isOpened()) {
                    CameraDialog.showDialog(this);
                } else {
                    mUVCCameraHandlerL.close();
                }
            }
        } else if (v.getId() == R.id.camera_layout_R) {
            if (mUVCCameraHandlerR != null) {
                if (!mUVCCameraHandlerR.isOpened()) {
                    CameraDialog.showDialog(this);
                } else {
                    mUVCCameraHandlerR.close();
                }
            }
        }
    }


    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_ENTER) {
            if(v.getId() == R.id.camera_layout_L) {

            } else if(v.getId() == R.id.camera_layout_R) {

            }
        }

        return false;
    }




    private void initView() {
        findViewById(R.id.rootLayout).setOnClickListener(this);

        mLeftCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_L);
        mLeftCameraLayout.setOnKeyListener(this);
        mLeftCameraLayout.setOnClickListener(this);
        mUVCCameraViewL = (CameraViewInterface) findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewL).setOnClickListener(this);
        mUVCCameraHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        mRightCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_R);
        mRightCameraLayout.setOnKeyListener(this);
        mRightCameraLayout.setOnClickListener(this);
        mUVCCameraViewR = (CameraViewInterface) findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewR).setOnClickListener(this);
        mUVCCameraHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);

    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Logger.getLogger().d("onAttach: " + device);
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(UsbDevice device) {
            Logger.getLogger().d("onDettach: " + device);
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Logger.getLogger().d("onConnect: " + device);
            if (!mUVCCameraHandlerL.isOpened()) {
                mUVCCameraHandlerL.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                mUVCCameraHandlerL.startPreview(new Surface(st));
            } else if (!mUVCCameraHandlerR.isOpened()) {
                mUVCCameraHandlerR.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                mUVCCameraHandlerR.startPreview(new Surface(st));
            }
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Logger.getLogger().d("onDisconnect: " + device);
            if ((mUVCCameraHandlerL != null) && !mUVCCameraHandlerL.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mUVCCameraHandlerL.close();
                        if (mLeftPrSurface != null) {
                            mLeftPrSurface.release();
                            mLeftPrSurface = null;
                        }
                    }
                }, 0);
            } else if ((mUVCCameraHandlerR != null) && !mUVCCameraHandlerR.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mUVCCameraHandlerR.close();
                        if (mRightPrSurface != null) {
                            mRightPrSurface.release();
                            mRightPrSurface = null;
                        }
                    }
                }, 0);
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            Logger.getLogger().d("onCancel: " + device);
        }
    };

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {

        }
    }

}
