package com.zx.tv.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
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
import com.zx.album.tv.GalleryActivityTv;
import com.zx.tv.camera.capture.Storage;
import com.zx.tv.camera.capture.Thumbnail;
import com.zx.tv.camera.gallery.FileInfo;
import com.zx.tv.camera.gallery.FileInfoManager;
import com.zx.tv.camera.gallery.FileLoader;
import com.zx.tv.camera.gallery.ThumbnailHelper;
import com.zx.tv.camera.utils.Exif;
import com.zx.tv.camera.utils.Logger;
import com.zx.tv.camera.utils.OnScreenHint;
import com.zx.tv.camera.utils.Util;
import com.zx.tv.camera.video.Encoder;
import com.zx.tv.camera.video.SurfaceEncoder;
import com.zx.tv.camera.widget.ModePicker;
import com.zx.tv.camera.widget.zxImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;


public class MainActivity extends BaseActivity implements View.OnClickListener
        , CameraDialog.CameraDialogParent {

    public static final int MODE_CAMERA = 0;
    public static final int MODE_VIDEO = 1;

    private static final int RECRODING_STOP = 0;
    private static final int RECRODING_PREPARE = 1;
    private static final int RECRODING_RUNNING = 2;

    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int UPDATE_THUMBNAIL = 7;

    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms

    private static final float[] BANDWIDTH_FACTORS = {0.5F, 0.5F};

    private static final int MAX_CONTINUE_PIC_NUM = 99;
    private static final int THUMBNAIL_WIDTH = 100;
    private static final int THUMBNAIL_HEIGHT = 100;

    private final Object mSync = new Object();

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera; // 拍照 录像

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

    private View mLargerCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerLarger;
    //    private SimpleUVCCameraTextureView mUVCCameraViewLarger;
    private CameraViewInterface mUVCCameraViewLarger;
    private Surface mLargerPrSurface;
    private TextView tvCameraPromateR;

    private USBMonitor.UsbControlBlock mUsbControlBlockL;
    private USBMonitor.UsbControlBlock mUsbControlBlockR;
    private USBMonitor.UsbControlBlock mUsbControlBlockLarger;

    private PrevSize leftPrevSize;
    private PrevSize rightPreSize;
    private PrevSize largerPreSize;

    private ModePicker mModePicker;
    private Chronometer mChronometer;
//    private ShutterButton mShutterButton;

    private SparseArray<USBMonitor.UsbControlBlock> mUsbControlBlockSparseArray = new SparseArray<>();

    private ContentResolver mContentResolver;

    private Thumbnail mThumbnail;

    private final Handler mHandler = new MainHandler();

    private boolean isLeft = false;
    private boolean isRight = false;
    private boolean isLarger = false;

    private boolean largerResume = false;

    private int mCurrrentMode = MODE_CAMERA;

    /**************video*****************/
    // Default 0. If it is larger than 0, the camcorder is in time lapse mode.
    private int mMaxVideoDurationInMs = 0;
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;
    private View mTimeLapseLabel;
    private LinearLayout mLabelsLinearLayout;
    private TextView mRecordingTimeView;
    private long mRecordingStartTime;
    private boolean mRecordingTimeCountsDown = false;

    private String mVideoFilename;
    private String mImageFilename;
    private ParcelFileDescriptor mVideoFileDescriptor;

    private String mCurrentVideoFilename;
    private String mCurrentImageFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;
    private ContentValues mCurrentImageValues;
    private Encoder mEncoder;
    private int mCaptureState = 0;


    //****************** 替换UI
    private ImageButton mSwitchModeButton;
    private ImageButton mShutterButton;
    private zxImageView mGalleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initCamera();

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.getLogger().d("*************** onResume");

        Logger.getLogger().i("**************** onStart isLarger " + isLarger);
        mUSBMonitor.register();
        if (isLarger) {
            if (mUVCCameraViewLarger != null) {
                mUVCCameraViewLarger.onResume();
            }
        } else {
            if (mUVCCameraViewL != null) {
                mUVCCameraViewL.onResume();
            }
            if (mUVCCameraViewR != null) {
                mUVCCameraViewR.onResume();
            }
        }
    }


    @Override
    protected void onStop() {
        Logger.getLogger().d("*************** onstop");
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
            largerResume = true;
        }

//        if (mUsbControlBlockL != null) {
//            mUsbControlBlockL = null;
//        }
//
//        if (mUsbControlBlockR != null) {
//            mUsbControlBlockR = null;
//        }
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

        switch (v.getId()) {

            case R.id.camera_layout_L:
                if (mUVCCameraHandlerL != null) {
                    if (!mUVCCameraHandlerL.isOpened()) {
                        CameraDialog.showDialog(this, mUSBMonitor);
                        isLeft = true;
                    } else {
                        mUVCCameraHandlerL.close();
                        openCaeralLarger(0);

                    }
                }
                break;
            case R.id.camera_layout_R:
                if (mUVCCameraHandlerR != null) {
                    if (!mUVCCameraHandlerR.isOpened()) {
                        CameraDialog.showDialog(this, mUSBMonitor);
                        isRight = true;
                    } else {
                        mUVCCameraHandlerR.close();
                        openCaeralLarger(1);
                    }
                }
                break;

            case R.id.imageButtonShutterMode:
                if (mCaptureState == RECRODING_RUNNING) {
                    return;
                }
                if (mCurrrentMode == MODE_CAMERA) {
                    mCurrrentMode = MODE_VIDEO;
                } else if (mCurrrentMode == MODE_VIDEO) {
                    mCurrrentMode = MODE_CAMERA;
                }
                updateModeSwitchIcon();
                break;

            case R.id.imageButtonActionShutter:
                onShutterButtonClick();
                break;
            case R.id.imageButtonGallery:
                if (FileInfoManager.getFileInfoList(FileLoader.CameraPath).size() != 0) {
//                    Intent intent = new Intent(this, GalleryMainActivity.class);
//                    startActivityForResult(intent, 0);
//                    Logger.getLogger().d("======== start GalleryMainActivity !!!!");
//

                    Intent intent = new Intent(this, GalleryActivityTv.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(GalleryActivityTv.KEY_INPUT_CHECKED_LIST_PATH, Storage.DIRECTORY);
                    bundle.putInt(GalleryActivityTv.KEY_INPUT_SELECT_INDEX, 0);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_data, Toast.LENGTH_SHORT).show();
                }
                break;
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

        mDualCameraView = findViewById(R.id.dual_camera_view);

        mLeftCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_L);
        mLeftCameraLayout.setOnClickListener(this);

        mUVCCameraViewL = (CameraViewInterface) findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewL).setOnClickListener(this);
        mUVCCameraHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateL = (TextView) findViewById(R.id.camera_open_promate_L);
        leftPrevSize = new PrevSize();



        mRightCameraLayout = (RelativeLayout) findViewById(R.id.camera_layout_R);
        mRightCameraLayout.setOnClickListener(this);
        mUVCCameraViewR = (CameraViewInterface) findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView) mUVCCameraViewR).setOnClickListener(this);
        mUVCCameraHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateR = (TextView) findViewById(R.id.camera_open_promate_R);
        rightPreSize = new PrevSize();



        mLargerCameraLayout = findViewById(R.id.camera_layout_larger);
        mLargerCameraLayout.setOnClickListener(this);
//        mUVCCameraViewLarger.setSurfaceTextureListener(mSurfaceTextureListener);
        mUVCCameraViewLarger = (CameraViewInterface) findViewById(R.id.camera_view_larger);

        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);

        mUVCCameraViewLarger.setAspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mUVCCameraHandlerLarger = UVCCameraHandler.createHandler(this, mUVCCameraViewLarger,
                displayMetrics.widthPixels, displayMetrics.heightPixels);
        mUVCCameraHandlerLarger.setCanAjustViewSize(true);// 自动去取合适分辨率

        largerPreSize = new PrevSize();

        mGalleryButton = (zxImageView) findViewById(R.id.imageButtonGallery);
        mGalleryButton.setOnClickListener(this);
        mSwitchModeButton = (ImageButton) findViewById(R.id.imageButtonShutterMode);
        mSwitchModeButton.setOnClickListener(this);
        mShutterButton = (ImageButton) findViewById(R.id.imageButtonActionShutter);
        mShutterButton.setOnClickListener(this);
        mShutterButton.setEnabled(false);

        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);
        mTimeLapseLabel = findViewById(R.id.time_lapse_label);
        mLabelsLinearLayout = (LinearLayout) findViewById(R.id.labels);

//
//        if(isLeft) {
//            mUVCCameraHandlerL = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraViewL,
//                    leftPrevSize.width, leftPrevSize.height);
//        } else if(isRight) {
//            mUVCCameraHandlerR = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraViewR,
//                    rightPreSize.width, rightPreSize.height);
//        } else if(isLarger) {
//            Logger.getLogger().i("width = " + largerPreSize.width + " largerPreSize.hight = "
//                    + largerPreSize.height);
//            mUVCCameraHandlerLarger = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraViewLarger,
//                    largerPreSize.width, largerPreSize.height);
//        }


        mUVCCameraViewL.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                leftPrevSize.width = width;
                leftPrevSize.height = height;

//                if(mUVCCameraHandlerL == null) {
//                    mUVCCameraHandlerL = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraViewL,
//                            leftPrevSize.width, leftPrevSize.height);
//                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {

            }
        });

        mUVCCameraViewR.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                Logger.getLogger().i("*********** larger onSurfaceCreated *** width = " + width
                            + "** heigh = " + height);
                rightPreSize.width = width;
                rightPreSize.height = height;
//                if(mUVCCameraHandlerR == null) {
//                    mUVCCameraHandlerR = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraViewR,
//                            rightPreSize.width, rightPreSize.height);
//                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {

            }
        });

        mUVCCameraViewLarger.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                largerPreSize.width = width;
                largerPreSize.height = height;
                if(mUVCCameraHandlerLarger != null) {
                    mUVCCameraHandlerLarger.setSize(width, height);
                }

                if (isLarger && largerResume) {
                    if (mUsbControlBlockLarger != null) {

                        mUVCCameraHandlerLarger.open(mUsbControlBlockLarger);
                        final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
                        mUVCCameraHandlerLarger.startPreview(new Surface(st));
                    }
                }
                Logger.getLogger().i("*********** larger onSurfaceCreated");
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {
                Logger.getLogger().i("*********** larger onSurfaceChanged");
            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                Logger.getLogger().i("*********** larger onSurfaceDestroy");
            }
        });


    }

    private void initCamera() {
//        mUVCCameraHandlerLarger.addCallback(new AbstractUVCCameraHandler.CameraCallback() {
//            @Override
//            public void onOpen() {
//                Logger.getLogger().d("camera onOpen");
//            }
//
//            @Override
//            public void onClose() {
//                Logger.getLogger().d("camera onClose");
//            }
//
//            @Override
//            public void onStartPreview() {
//                Logger.getLogger().d("camera onStartPreview");
//
//            }
//
//            @Override
//            public void onStopPreview() {
//                Logger.getLogger().d("camera onStopPreview");
//            }
//
//            @Override
//            public void onStartRecording() {
//                Logger.getLogger().d("camera onStartRecording");
//            }
//
//            @Override
//            public void onStopRecording() {
//                Logger.getLogger().d("camera onStopRecording");
//            }
//
//            @Override
//            public void onCaptureFinish() {
//                Logger.getLogger().d("camera cpature onCaptureFinish");
//                mHandler.removeMessages(UPDATE_THUMBNAIL);
//
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Logger.getLogger().e("camera " + e);
//                Util.showError(MainActivity.this, R.string.cannot_connect_camera);
//            }
//        });
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Logger.getLogger().d("onAttach: " + device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            (device.getProductName() != null) ? device.getProductName() : ""  + getString(R.string.device_attach),
                            Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onDettach(final UsbDevice device) {
            Logger.getLogger().d("onDettach: " + device);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            (device.getProductName() != null) ? device.getProductName() : ""  + getString(R.string.device_dettach),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Logger.getLogger().d("onConnect: " + device);

            if (isLeft == true && !mUVCCameraHandlerL.isOpened()) {
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
                isLeft = false;
                mUsbControlBlockSparseArray.put(0, ctrlBlock);
            } else if (isRight == true && !mUVCCameraHandlerR.isOpened()) {
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
                isRight = false;
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
                        isLeft = false;
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
                        isRight = false;
                    }
                }, 0);
            } else if ((mUVCCameraHandlerLarger != null) && !mUVCCameraHandlerLarger.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mUVCCameraHandlerLarger.close();
                        synchronized (mSync) {
                            if (mUVCCamera != null) {
                                mUVCCamera.close();
                            }
                        }
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
            isLeft = false;
            isRight = false;
        }
    }

    public void updateModeSwitchIcon() {
        Logger.getLogger().d("************* updateModeSwitchIcon " + mCurrrentMode);
        if (mCurrrentMode == MODE_VIDEO) {
            mSwitchModeButton.setImageResource(R.drawable.btn_mode_camera);
            mShutterButton.setImageResource(R.drawable.btn_shutter_video);
        } else if (mCurrrentMode == MODE_CAMERA) {
            mSwitchModeButton.setImageResource(R.drawable.btn_mode_video);
            mShutterButton.setImageResource(R.drawable.btn_shutter);
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

//        mShutterButton.setBackgroundResource(R.drawable.btn_shutter_video);
//        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.requestFocus();

        queueEvent(new Runnable() {
            @Override
            public void run() {
//                USBMonitor.UsbControlBlock mUsbControlBlockLarger = mUsbControlBlockSparseArray.get(index);
                mUsbControlBlockLarger = mUsbControlBlockSparseArray.get(index);
                if (null != mUsbControlBlockLarger) {
                    if (!mUVCCameraHandlerLarger.isOpened()) {
                        mUVCCameraHandlerLarger.open(mUsbControlBlockLarger);
                        final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
                        mUVCCameraHandlerLarger.startPreview(new Surface(st));
                    }
                }
            }
        }, 100);

//        synchronized (mSync) {
//            if (mUVCCamera != null) {
//                mUVCCamera.destroy();
//                mUVCCamera = null;
//            }
//        }
// ******************* 采取库里编码方式录像 ，统一接口管理

//        queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                final UVCCamera camera = new UVCCamera();
//                USBMonitor.UsbControlBlock ctrlBlock = mUsbControlBlockSparseArray.get(index);
//                camera.open(ctrlBlock);
//                if (mLargerPrSurface != null) {
//                    mLargerPrSurface.release();
//                    mLargerPrSurface = null;
//                }
//                try {
//                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
//                } catch (final IllegalArgumentException e) {
//                    try {
//                        // fallback to YUV mode
//                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
//                    } catch (final IllegalArgumentException e1) {
//                        camera.destroy();
//                        Util.showError(MainActivity.this, R.string.cannot_connect_camera);
//                        return;
//                    }
//                }
//                final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
//                if (st != null) {
//                    mLargerPrSurface = new Surface(st);
//                    camera.setPreviewDisplay(mLargerPrSurface);
//                    camera.startPreview();
//
//                }
//                synchronized (mSync) {
//                    mUVCCamera = camera;
//                }
//            }
//        }, 0);

        mShutterButton.setEnabled(true);
        findViewById(R.id.mode_picker).setVisibility(View.VISIBLE);
    }

    private void resumeCameraDual() {
        isLarger = false;
        mDualCameraView.setVisibility(View.VISIBLE);
        mLargerCameraLayout.setVisibility(View.GONE);
        mUVCCameraHandlerLarger.close();

//        releaseLargerCamera();

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

        final USBMonitor.UsbControlBlock rightControlBlock = mUsbControlBlockSparseArray.get(1);
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

    //**********************************************************************
//    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
//
//        @Override
//        public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
//            if (mLargerPrSurface != null) {
//                mLargerPrSurface.release();
//                mLargerPrSurface = null;
//            }
//            return true;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
//            if (mEncoder != null && mCaptureState == t RECRODING_RUNNING){
//                mEncoder.frameAvailable();
//            }
//        }
//    };

    /**
     * start capturing
     */
    private final void startRecroding() {
        if (mCurrrentMode == MODE_VIDEO && mCaptureState == RECRODING_STOP) {
            mCaptureState = RECRODING_RUNNING;
            updateAndShowStorageHint();
            if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
                Logger.getLogger().e("Storage issue, ignore the start request");
                return;
            }
            mCurrentVideoUri = null;

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    generateVideoFilename(MediaRecorder.OutputFormat.MPEG_4);
                    Logger.getLogger().d("**************Videoname = " + mVideoFilename);
                    final String path = mVideoFilename;
                    if (!TextUtils.isEmpty(path)) {
//                        mEncoder = new SurfaceEncoder(path);
//                        mEncoder.setEncodeListener(mEncodeListener);
//                        try {
//                            mEncoder.prepare();
//                            mEncoder.startRecording();
//                        } catch (final IOException e) {
//                            mCaptureState = RECRODING_STOP;
//                        }
                        mUVCCameraHandlerLarger.startRecording(path);
                    } else
                        throw new RuntimeException("Failed to start capture. vieopath is null");
                }
            }, 0);


            mRecordingStartTime = SystemClock.uptimeMillis();
            showRecordingUI(true);

            updateRecordingTime();
        }
    }

    /**
     * stop capture if capturing
     */
    private final void stopRecroding() {
        if (mCurrrentMode == MODE_VIDEO &&
                mCaptureState == RECRODING_RUNNING) {

            queueEvent(new Runnable() {
                @Override
                public void run() {
//                synchronized (mSync) {
//                    if (mUVCCamera != null) {
//                        mUVCCamera.stopCapture();
//                    }
//                }
//                if (mEncoder != null) {
//                    mEncoder.stopRecording();
//                    mEncoder = null;
//                }
                    mUVCCameraHandlerLarger.stopRecording();
                }
            }, 0);
            mCaptureState = RECRODING_STOP;
            mCurrentVideoFilename = mVideoFilename;
            showRecordingUI(false);
            addVideoToMediaStore();
        }

    }

    private void releaseLargerCamera() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.close();
                    }
                }
                if (mLargerPrSurface != null) {
                    mLargerPrSurface.release();
                    mLargerPrSurface = null;
                }
            }
        }, 0);
    }

    /**
     * callbackds from Encoder
     */
    private final Encoder.EncodeListener mEncodeListener = new Encoder.EncodeListener() {
        @Override
        public void onPreapared(final Encoder encoder) {
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.startCapture(((SurfaceEncoder) encoder).getInputSurface());
                }
            }
            mCaptureState = RECRODING_RUNNING;
        }

        @Override
        public void onRelease(final Encoder encoder) {
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.stopCapture();
                }
            }
            mCaptureState = RECRODING_STOP;
        }
    };

    private void generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        // Used when emailing.
        String filename = title + Util.convertOutputFormatToFileExt(outputFileFormat);
        String mime = Util.convertOutputFormatToMimeType(outputFileFormat);
        mVideoFilename = Storage.DIRECTORY + '/' + filename;

        mCurrentVideoValues = new ContentValues(7);
        mCurrentVideoValues.put(MediaStore.Video.Media.TITLE, title);
        mCurrentVideoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(MediaStore.Video.Media.MIME_TYPE, "video/avc");
        mCurrentVideoValues.put(MediaStore.Video.Media.DATA, mVideoFilename);
        mCurrentVideoValues.put(MediaStore.Video.Media.RESOLUTION,
                Integer.toString(SurfaceEncoder.FRAME_WIDTH) + "x" +
                        Integer.toString(SurfaceEncoder.FRAME_WIDTH));
        Logger.getLogger().v("New video filename: " + mVideoFilename);
    }

    private void generateImageFilaname(String ext) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filenmae = title + ext;
        mImageFilename = Storage.DIRECTORY + '/' + filenmae;
        mCurrentImageFilename = mImageFilename;

        mCurrentImageValues = new ContentValues(9);
        mCurrentImageValues.put(MediaStore.Images.ImageColumns.TITLE, title);
        mCurrentImageValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ext);
        mCurrentImageValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        mCurrentImageValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
        mCurrentImageValues.put(MediaStore.Images.ImageColumns.DATA, mImageFilename);
//        mCurrentImageValues.put(MediaStore.Images.ImageColumns.SIZE, jpeg.length);
//        mCurrentImageValues.put(MediaStore.Images.ImageColumns.WIDTH, width);
//        mCurrentImageValues.put(MediaStore.Images.ImageColumns.HEIGHT, height);
        Logger.getLogger().v("New image filename: " + mCurrentImageFilename);
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    private void updateImageThumbnailView() {
        String path = mCurrentImageFilename;
        if (TextUtils.isEmpty(path)) {
            return;
        }

        mHandler.removeMessages(UPDATE_THUMBNAIL);

        ArrayList<FileInfo> f_list = FileInfoManager.getFileInfoList(Storage.DIRECTORY);
        FileInfo f = null;
        if ((f_list == null) || (f_list.size() <= 0)) {
            mGalleryButton.setBackgroundResource(R.drawable.grey);
            return;
        }

        f = f_list.get(0);

        if (f == null) {
            mGalleryButton.setBackgroundResource(R.drawable.grey);
            return;
        }

        if (f.getMineType().equals("image/*"))
            mGalleryButton.setImageBitmap(ThumbnailHelper.getImageThumbnail(f.getPath(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
        else if (f.getMineType().equals("video/*"))
            mGalleryButton.setImageBitmap(ThumbnailHelper.getVideoThumbnail(f.getPath(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, MediaStore.Images.Thumbnails.MICRO_KIND));
        else
            Logger.getLogger().e("error mRecentMediaType : " + f.getMineType());
    }

    private void closeVideoFileDescriptor() {
        if (mVideoFileDescriptor != null) {
            try {
                mVideoFileDescriptor.close();
            } catch (IOException e) {
                Logger.getLogger().e("Fail to close fd" + e);
            }
            mVideoFileDescriptor = null;
        }
    }

    private static final String getCaptureFile(final String type, final String ext) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), "USBCameraTest");
        dir.mkdirs();    // create directories if they do not exist
        if (dir.canWrite()) {
            return (new File(dir, getDateTimeString() + ext)).toString();
        }
        return null;
    }

    private static final SimpleDateFormat sDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return sDateTimeFormat.format(now.getTime());
    }

    private void addVideoToMediaStore() {
        if (mVideoFileDescriptor == null) {
            Uri videoTable = Uri.parse("content://media/external/video/media");
            mCurrentVideoValues.put(MediaStore.Video.Media.SIZE,
                    new File(mCurrentVideoFilename).length());
            long duration = SystemClock.uptimeMillis() - mRecordingStartTime;
            if (duration > 0) {
                mCurrentVideoValues.put(MediaStore.Video.Media.DURATION, duration);
            } else {
                Logger.getLogger().v("Video duration <= 0 : " + duration);
            }
            try {
                mCurrentVideoUri = mContentResolver.insert(videoTable,
                        mCurrentVideoValues);
                sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO,
                        mCurrentVideoUri));
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                mCurrentVideoUri = null;
                mCurrentVideoFilename = null;
            } finally {
                Logger.getLogger().v("Current video URI: " + mCurrentVideoUri);
            }
        }
        mCurrentVideoValues = null;
    }

    private void deleteCurrentVideo() {
        // Remove the video and the uri if the uri is not passed in by intent.
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
            if (mCurrentVideoUri != null) {
                mContentResolver.delete(mCurrentVideoUri, null, null);
                mCurrentVideoUri = null;
            }
        }
        updateAndShowStorageHint();
    }

    private void deleteVideoFile(String fileName) {
        Logger.getLogger().d("Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Logger.getLogger().d("Could not delete " + fileName);
        }
    }

    private OnScreenHint mStorageHint;
    private long mStorageSpace;

    private void updateAndShowStorageHint() {
        mStorageSpace = Storage.getAvailableSpace();
        Logger.getLogger().d("***************** mStorageSpace " + mStorageSpace);
        showStorageHint();
    }

    private void showStorageHint() {
        String errorMessage = null;
        if (mStorageSpace == Storage.UNAVAILABLE) {
            errorMessage = getString(R.string.no_storage);
        } else if (mStorageSpace == Storage.PREPARING) {
            errorMessage = getString(R.string.preparing_sd);
        } else if (mStorageSpace == Storage.UNKNOWN_SIZE) {
            errorMessage = getString(R.string.access_sd_fail);
        } else if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
            errorMessage = getString(R.string.spaceIsLow_content);
        }

        if (errorMessage != null) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(this, errorMessage);
            } else {
                mStorageHint.setText(errorMessage);
            }
            mStorageHint.show();
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    private void showRecordingUI(boolean recording) {
        Logger.getLogger().i("******************showRecordingUI******" + recording);
        if (recording) {
//            if (mThumbnailView != null) mThumbnailView.setEnabled(false);
            mShutterButton.setImageResource(R.drawable.btn_shutter_video_recording);
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
        } else {
//            if (mThumbnailView != null) mThumbnailView.setEnabled(true);
            mShutterButton.setImageResource(R.drawable.btn_shutter_video);
//            mShutterButton.setEnabled(true);
            mRecordingTimeView.setVisibility(View.GONE);
        }
    }

    private long getTimeLapseVideoLength(long deltaMs) {
        // For better approximation calculate fractional number of frames captured.
        // This will update the video time at a higher resolution.
        double numberOfFrames = (double) deltaMs / mTimeBetweenTimeLapseFrameCaptureMs;
        return (long) (numberOfFrames / 15 * 1000);// surfaceEncoder 定义
    }

    private void updateRecordingTime() {
        if (mCaptureState != RECRODING_RUNNING) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0
                && delta >= mMaxVideoDurationInMs - 60000);

        long deltaAdjusted = delta;
        if (countdownRemainingTime) {
            deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
        }
        String text;

        long targetNextUpdateDelay;

        text = Util.millisecondToTimeString(deltaAdjusted, false);
        targetNextUpdateDelay = 1000;


        mRecordingTimeView.setText(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = getResources().getColor(countdownRemainingTime
                    ? R.color.recording_time_remaining_text
                    : R.color.recording_time_elapsed_text);

            mRecordingTimeView.setTextColor(color);
        }

        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    private void onShutterButtonClick() {
        if (mCurrrentMode == MODE_VIDEO) {
            boolean stop = (mCaptureState == RECRODING_RUNNING);
            if (stop) {
                stopRecroding();
            } else {
                startRecroding();
            }
            mShutterButton.setEnabled(false);
            Logger.getLogger().i("*******onShutterButtonClick***********stop " + stop);
            // 防止快速多次点击
            if (stop) {

            }
            mHandler.sendEmptyMessageDelayed(
                    ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);

        } else if (mCurrrentMode == MODE_CAMERA) {
            updateAndShowStorageHint();
            if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
                Logger.getLogger().e("Storage issue, ignore the start request");
                return;
            }

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    generateImageFilaname(".png");
                    Logger.getLogger().d("**************image name = " + mImageFilename);
                    final String path = mImageFilename;
                    if (!TextUtils.isEmpty(path)) {
                        mUVCCameraHandlerLarger.captureStill(path);
                        mHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                    } else {
                        throw new RuntimeException("Failed to start capture. vieopath is null");
                    }
                }
            }, 0);

        }
    }

    //***************************image*******************************************

    private class MainHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_RECORD_TIME:
                    updateRecordingTime();
                    break;
                case UPDATE_THUMBNAIL:
                    updateImageThumbnailView();
                    break;
                case ENABLE_SHUTTER_BUTTON:
                    mShutterButton.setEnabled(true);
                    break;
            }
        }
    }

    private class ImageSaver extends Thread {

        private static final int QUEUE_LIMIT = 3;

        private ArrayList<SaveRequest> mQueue;
        private Thumbnail mPendingThumbnail;
        private Object mUpdateThumbnailLock = new Object();
        private boolean mStop;

        public ImageSaver() {
            mQueue = new ArrayList<>();
            start();
        }

        public void addImage(final byte[] data, int width, int height) {
            SaveRequest r = new SaveRequest();
            r.data = data;
            r.width = width;
            r.height = height;
            r.dateTaken = System.currentTimeMillis();
            synchronized (this) {
                while (mQueue.size() >= QUEUE_LIMIT) {
                    try {
                        wait();
                    } catch (InterruptedException e) {

                    }
                }
                mQueue.add(r);
                notifyAll();
            }
        }

        @Override
        public void run() {
            while (true) {
                SaveRequest r;
                synchronized (this) {
                    if (mQueue.isEmpty()) {
                        notifyAll();

                        if (mStop) {
                            break;
                        }

                        try {
                            wait();
                        } catch (InterruptedException e) {

                        }
                        continue;
                    }
                    r = mQueue.get(0);
                }
                storeImage(r.data, r.width, r.height, r.dateTaken,
                        r.previewWidth);
                synchronized (this) {
                    mQueue.remove(0);
                    notifyAll();
                }
            }


        }

        public void waitDone() {
            synchronized (this) {
                while (!mQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {

                    }
                }
            }
            updateThumbnail();
        }

        public void finish() {
            waitDone();
            synchronized (this) {
                mStop = true;
                notifyAll();
            }
            try {
                join();
            } catch (InterruptedException e) {

            }
        }

        private void updateThumbnail() {
            Thumbnail t;
            synchronized (mUpdateThumbnailLock) {
                mHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                t = mPendingThumbnail;
                mPendingThumbnail = null;
            }

            if (t != null) {
                mThumbnail = t;

            }

        }

        private void storeImage(final byte[] data, int width, int height, long dateTaken, int previewWidth) {
            String title = Util.createJpegName(dateTaken);
            int orientation = Exif.getOrientation(data);
            Uri uri = Storage.addImage(mContentResolver, title, dateTaken, orientation, data, width, height);
            if (null != uri) {
                boolean needThumbnail;
                synchronized (this) {
                    needThumbnail = (mQueue.size() <= 1);
                }
                if (needThumbnail) {
                    int ratio = (int) Math.ceil((double) width / previewWidth);
                    int inSampleSize = Integer.highestOneBit(ratio);
                    Thumbnail t = Thumbnail.createThumbnail(data, orientation, inSampleSize, uri);
                    synchronized (mUpdateThumbnailLock) {
                        mPendingThumbnail = t;
                        mHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                    }
                }
            }
            Util.broadcastNewPicture(MainActivity.this, uri);

        }
    }

    private static class SaveRequest {
        byte[] data;
        int width, height;
        long dateTaken;
        int previewWidth;
    }

    private static class PrevSize {
        public int width;
        public int height;
    }

}
