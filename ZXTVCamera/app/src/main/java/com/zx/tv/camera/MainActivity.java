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
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.AbstractUVCCameraHandler;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.zx.tv.camera.capture.Storage;
import com.zx.tv.camera.capture.Thumbnail;
import com.zx.tv.camera.utils.Exif;
import com.zx.tv.camera.utils.Logger;
import com.zx.tv.camera.utils.OnScreenHint;
import com.zx.tv.camera.utils.Util;
import com.zx.tv.camera.video.Encoder;
import com.zx.tv.camera.video.SurfaceEncoder;
import com.zx.tv.camera.widget.ModePicker;
import com.zx.tv.camera.widget.ShutterButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;



public class MainActivity extends BaseActivity implements View.OnClickListener
        , CameraDialog.CameraDialogParent, ModePicker.OnModeChangeListener
        , ShutterButton.OnShutterButtonListener {

    private static final int RECRODING_STOP = 0;
    private static final int RECRODING_PREPARE = 1;
    private static final int RECRODING_RUNNING = 2;

    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int UPDATE_THUMBNAIL = 7;

    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms

    private static final float[] BANDWIDTH_FACTORS = {0.5F, 0.5F};

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

    private LinearLayout mLargerCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerLarger;
    //    private SimpleUVCCameraTextureView mUVCCameraViewLarger;
    private CameraViewInterface mUVCCameraViewLarger;
    private Surface mLargerPrSurface;
    private TextView tvCameraPromateR;

    private USBMonitor.UsbControlBlock mUsbControlBlockL;
    private USBMonitor.UsbControlBlock mUsbControlBlockR;

    private ModePicker mModePicker;
    private Chronometer mChronometer;
    private ShutterButton mShutterButton;

    private SparseArray<USBMonitor.UsbControlBlock> mUsbControlBlockSparseArray = new SparseArray<>();

    private ContentResolver mContentResolver;

    private Thumbnail mThumbnail;

    private final Handler mHandler = new MainHandler();

    private boolean isLarger;

    private int mCurrrentMode = ModePicker.MODE_CAMERA;

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
//                if (mUVCCameraHandlerLarger != null) {
//                    if (!mUVCCameraHandlerLarger.isOpened()) {
//                        CameraDialog.showDialog(this);
//                    } else {
//                        mUVCCameraHandlerLarger.close();
//                    }
//                }
        } else if (v.getId() == R.id.shutter_button) {
            if (mCurrrentMode == ModePicker.MODE_CAMERA) {

            } else if (mCurrrentMode == ModePicker.MODE_VIDEO) {
                if (checkPermissionWriteExternalStorage()) {
                    if (mCaptureState == RECRODING_STOP) {
                        startRecroding();
                    } else {
                        stopRecroding();
                    }
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
//        mUVCCameraViewLarger = (SimpleUVCCameraTextureView) findViewById(R.id.camera_view_larger);
//        mUVCCameraViewLarger.setSurfaceTextureListener(mSurfaceTextureListener);
        mUVCCameraViewLarger = (CameraViewInterface) findViewById(R.id.camera_view_larger);

//        mUVCCameraViewLarger.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraHandlerLarger = UVCCameraHandler.createHandler(this, mUVCCameraViewLarger,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        mModePicker = (ModePicker) findViewById(R.id.mode_picker);
        mModePicker.setOnModeChangeListener(this);

        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mChronometer.setVisibility(View.GONE);
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setEnabled(false);


        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);
        mTimeLapseLabel = findViewById(R.id.time_lapse_label);
        mLabelsLinearLayout = (LinearLayout) findViewById(R.id.labels);
    }

    private void initCamera() {
        mUVCCameraHandlerLarger.addCallback(new AbstractUVCCameraHandler.CameraCallback() {
            @Override
            public void onOpen() {
                Logger.getLogger().d("camera onOpen");
            }

            @Override
            public void onClose() {
                Logger.getLogger().d("camera onClose");
            }

            @Override
            public void onStartPreview() {
                Logger.getLogger().d("camera onStartPreview");
                mModePicker.setCurrentMode(mCurrrentMode);
                mModePicker.setEnabled(true);
                mModePicker.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopPreview() {
                Logger.getLogger().d("camera onStopPreview");
            }

            @Override
            public void onStartRecording() {
                Logger.getLogger().d("camera onStartRecording");
            }

            @Override
            public void onStopRecording() {
                Logger.getLogger().d("camera onStopRecording");
            }

            @Override
            public void onCaptureFinish() {
                Logger.getLogger().d("camera cpature onCaptureFinish");
                mHandler.removeMessages(UPDATE_THUMBNAIL);

            }

            @Override
            public void onError(Exception e) {
                Logger.getLogger().e("camera " + e);
                Util.showError(MainActivity.this, R.string.cannot_connect_camera);
            }
        });
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

        mShutterButton.setBackgroundResource(R.drawable.btn_shutter_video);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.requestFocus();

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
//        mUVCCameraHandlerLarger.close();

        releaseLargerCamera();

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

    @Override
    public boolean onModeChanged(int newMode) {
        return false;
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
        if (mCurrrentMode == ModePicker.MODE_VIDEO && mCaptureState == RECRODING_STOP) {
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
        if (mCurrrentMode == ModePicker.MODE_VIDEO &&
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
        if (recording) {
//            if (mThumbnailView != null) mThumbnailView.setEnabled(false);
            mShutterButton.setBackgroundResource(R.drawable.btn_shutter_video_recording);
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
        } else {
//            if (mThumbnailView != null) mThumbnailView.setEnabled(true);
            mShutterButton.setBackgroundResource(R.drawable.btn_shutter_video);
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

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {
        if (mCurrrentMode == ModePicker.MODE_VIDEO) {
            boolean stop = (mCaptureState == RECRODING_RUNNING);
            if (stop) {
                stopRecroding();
            } else {
                startRecroding();
            }
            mShutterButton.setEnabled(false);

            // Keep the shutter button disabled when in video capture intent
            // mode and recording is stopped. It'll be re-enabled when
            // re-take button is clicked.
            if (!stop) {
                mHandler.sendEmptyMessageDelayed(
                        ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);
            }
        } else if(mCurrrentMode == ModePicker.MODE_CAMERA) {
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
                    } else
                        throw new RuntimeException("Failed to start capture. vieopath is null");
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

}
