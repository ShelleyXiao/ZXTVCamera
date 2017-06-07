package com.zx.tv.camera;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.zx.tv.camera.utils.Logger;

public class MainActivity extends BaseActivity implements View.OnClickListener
        , CameraDialog.CameraDialogParent {

    private static final float[] BANDWIDTH_FACTORS = {0.5F, 0.5F};

    private USBMonitor mUSBMonitor;

    private View mDualCameraView;

    private RelativeLayout mLeftCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerL;
    private CameraViewInterface mUVCCameraViewR;
    private Surface mLeftPrSurface;
    private TextView tvCameraPromateL;

    private RelativeLayout mRightCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerR;
    private CameraViewInterface mUVCCameraViewL;
    private Surface mRightPrSurface;

    private LinearLayout mLargerCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerLarger;
    private CameraViewInterface mUVCCameraViewLarger;
    private Surface mLargerPrSurface;
    private TextView tvCameraPromateR;

    private USBMonitor.UsbControlBlock mUsbControlBlockL;
    private USBMonitor.UsbControlBlock mUsbControlBlockR;

    private SparseArray<USBMonitor.UsbControlBlock> mUsbControlBlockSparseArray = new SparseArray<>();

    private boolean isLarger;


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

        mUVCCameraHandlerLarger.close();
        if (mUVCCameraHandlerLarger != null) {
            mUVCCameraViewLarger.onPause();
        }

        if (mUsbControlBlockL != null) {
            mUsbControlBlockL = null;
        }

        if (mUsbControlBlockR != null) {
            mUsbControlBlockR = null;
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

        if (mUVCCameraHandlerLarger != null) {
            mUVCCameraHandlerLarger = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUsbControlBlockSparseArray.clear();

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
                    openCaeralLarger(0);

                }
            }
        } else if (v.getId() == R.id.camera_layout_R) {
            if (mUVCCameraHandlerR != null) {
                if (!mUVCCameraHandlerR.isOpened()) {
                    CameraDialog.showDialog(this);
                } else {
                    mUVCCameraHandlerR.close();
                    openCaeralLarger(1);
                }
            }
        } else if (v.getId() == R.id.camera_layout_larger) {
            if (mUVCCameraHandlerLarger != null) {
                if (!mUVCCameraHandlerLarger.isOpened()) {
                    CameraDialog.showDialog(this);
                } else {
                    mUVCCameraHandlerLarger.close();
                }
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isLarger) {
                resumeCameraDual();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
//        findViewById(R.id.rootLayout).setOnClickListener(this);

        mDualCameraView = findViewById(R.id.dual_camera_view);

        mLeftCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_L);
        mLeftCameraLayout.setOnClickListener(this);

        mUVCCameraViewL = (CameraViewInterface) findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewL).setOnClickListener(this);
        mUVCCameraHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateL = (TextView) findViewById(R.id.camera_open_promate_L);

        mRightCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_R);
        mRightCameraLayout.setOnClickListener(this);
        mUVCCameraViewR = (CameraViewInterface) findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewR).setOnClickListener(this);
        mUVCCameraHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateR = (TextView) findViewById(R.id.camera_open_promate_R);

        mLargerCameraLayout = (LinearLayout) findViewById(R.id.camera_layout_larger);
        mLargerCameraLayout.setOnClickListener(this);
        mUVCCameraViewLarger = (CameraViewInterface) findViewById(R.id.camera_view_larger);
//        mUVCCameraViewLarger.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraHandlerLarger = UVCCameraHandler.createHandler(this, mUVCCameraViewLarger,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);


    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Logger.getLogger().d("onAttach: " + device);
            Toast.makeText(MainActivity.this, device.getProductName() + getString(R.string.device_attach), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(UsbDevice device) {
            Logger.getLogger().d("onDettach: " + device);
            Toast.makeText(MainActivity.this, device.getProductName() + getString(R.string.device_dettach), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Logger.getLogger().d("onConnect: " + device);
            if (!mUVCCameraHandlerL.isOpened()) {
                mUVCCameraHandlerL.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                mUVCCameraHandlerL.startPreview(new Surface(st));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCameraPromateL.setVisibility(View.GONE);
                    }
                });
                isLarger = false;
                mUsbControlBlockSparseArray.put(0, ctrlBlock);
            } else if (!mUVCCameraHandlerR.isOpened()) {
                mUVCCameraHandlerR.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                mUVCCameraHandlerR.startPreview(new Surface(st));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCameraPromateR.setVisibility(View.GONE);
                    }
                }, 0);
                isLarger = false;
                mUsbControlBlockSparseArray.put(1, ctrlBlock);
            } else if (!mUVCCameraHandlerLarger.isOpened()) {
                mUVCCameraHandlerLarger.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
                mUVCCameraHandlerLarger.startPreview(new Surface(st));
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
            } else if ((mUVCCameraHandlerLarger != null) && !mUVCCameraHandlerLarger.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mUVCCameraHandlerLarger.close();
                        if (mLargerPrSurface != null) {
                            mLargerPrSurface.release();
                            mLargerPrSurface = null;
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

    /**
     * index = 0 ---> left
     * index = 1 ---> right
     */
    private void openCaeralLarger(final int index) {
        isLarger = true;
        mDualCameraView.setVisibility(View.GONE);
        mLargerCameraLayout.setVisibility(View.VISIBLE);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                USBMonitor.UsbControlBlock controlBlock = mUsbControlBlockSparseArray.get(index);
                if (null != controlBlock) {
                    if (!mUVCCameraHandlerLarger.isOpened()) {
                        mUVCCameraHandlerLarger.open(controlBlock);
                        final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
                        mUVCCameraHandlerLarger.startPreview(new Surface(st));
                    }
                }
            }
        }, 100);

        findViewById(R.id.mode_picker).setVisibility(View.VISIBLE);
    }

    private void resumeCameraDual() {
        isLarger = false;
        mDualCameraView.setVisibility(View.VISIBLE);
        mLargerCameraLayout.setVisibility(View.GONE);
        mUVCCameraHandlerLarger.close();

        final USBMonitor.UsbControlBlock leftControlBlock = mUsbControlBlockSparseArray.get(0);
        if (null != leftControlBlock) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (!mUVCCameraHandlerL.isOpened()) {
                        mUVCCameraHandlerL.open(leftControlBlock);
                        final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                        mUVCCameraHandlerL.startPreview(new Surface(st));
                    }
                }
            }, 100);
        }

        final USBMonitor.UsbControlBlock rightControlBlock = mUsbControlBlockSparseArray.get(0);
        if (null != rightControlBlock) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (!mUVCCameraHandlerR.isOpened()) {
                        mUVCCameraHandlerR.open(rightControlBlock);
                        final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                        mUVCCameraHandlerR.startPreview(new Surface(st));
                    }
                }
            }, 100);
        }

    }

}
