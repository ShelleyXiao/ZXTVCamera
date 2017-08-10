package com.zx.tv.camera.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.zx.album.tv.GalleryActivityTv;
import com.zx.tv.camera.R;
import com.zx.tv.camera.capture.Storage;
import com.zx.tv.camera.gallery.FileInfo;
import com.zx.tv.camera.gallery.FileInfoManager;
import com.zx.tv.camera.gallery.FileLoader;
import com.zx.tv.camera.gallery.ThumbnailHelper;
import com.zx.tv.camera.utils.Logger;
import com.zx.tv.camera.utils.OnScreenHint;
import com.zx.tv.camera.utils.Util;
import com.zx.tv.camera.video.Encoder;
import com.zx.tv.camera.video.SurfaceEncoder;
import com.zx.tv.camera.widget.zxImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * User: ShaudXiao
 * Date: 2017-08-07
 * Time: 11:16
 * Company: zx
 * Description:
 * FIXME
 */


public class PreviewFragment extends BaseFragment implements View.OnClickListener{

    public static final int MODE_CAMERA = 0;
    public static final int MODE_VIDEO = 1;

    private static final int RECRODING_STOP = 0;
    private static final int RECRODING_PREPARE = 1;
    private static final int RECRODING_RUNNING = 2;

    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int UPDATE_THUMBNAIL = 7;

    private static final int MAX_CONTINUE_PIC_NUM = 99;
    private static final int THUMBNAIL_WIDTH = 100;
    private static final int THUMBNAIL_HEIGHT = 100;

    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera; // 拍照 录像
    private final Object mSync = new Object();
    private USBMonitor.UsbControlBlock mUsbControlBlockLarger;

    private DisplayFragment.PrevSize largerPreSize;

    private View mLargerCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerLarger;
    //    private SimpleUVCCameraTextureView mUVCCameraViewLarger;
    private CameraViewInterface mUVCCameraViewLarger;
    private Surface mLargerPrSurface;

    private String mDefaultDeviceName;
    private UsbDevice mDefaultDevice;

    private ImageButton mSwitchModeButton;
    private ImageButton mShutterButton;
    private zxImageView mGalleryButton;

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

    private ContentResolver mContentResolver;
    private String mCurrentVideoFilename;
    private String mCurrentImageFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;
    private ContentValues mCurrentImageValues;
    private Encoder mEncoder;
    private int mCaptureState = 0;



    private MainHandler mHandler = new MainHandler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mDefaultDeviceName = bundle.getString("devicename");

    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.preview_fragment, container, false);

        initView(view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUSBMonitor = new USBMonitor(getActivity(), mOnDeviceConnectListener);
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
        List<UsbDevice> deviceList = mUSBMonitor.getDeviceList(filter.get(0));
        Logger.getLogger().i("***********mDefaultDeviceName " + mDefaultDeviceName + "********************************" + deviceList.size());
        for(UsbDevice device : deviceList) {
            String id = String.format("UVC Camera:(%x:%x:%s)", device.getVendorId(), device.getProductId(), device.getDeviceName());
            Logger.getLogger().i("id: " + id);
            if(id.equals(mDefaultDeviceName)) {
                mDefaultDevice = device;
                break;
            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraViewLarger != null) {
            mUVCCameraViewLarger.onResume();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        mShutterButton.requestFocus();
    }

    @Override
    public void onStop() {
        mUVCCameraHandlerLarger.close();
        if (mUVCCameraViewLarger != null)
            mUVCCameraViewLarger.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mUVCCameraHandlerLarger != null) {
            mUVCCameraHandlerLarger.release();
            mUVCCameraHandlerLarger = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraViewLarger = null;
        super.onDestroy();
    }

    public static PreviewFragment newInstance() {
        PreviewFragment instance = new PreviewFragment();
        return instance;
    }

    private void initView(View view) {

        mLargerCameraLayout = view.findViewById(R.id.camera_layout_larger);
//        mUVCCameraViewLarger.setSurfaceTextureListener(mSurfaceTextureListener);
        mUVCCameraViewLarger = (CameraViewInterface) view.findViewById(R.id.camera_view_larger);

        WindowManager wm = getActivity().getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);

        mUVCCameraViewLarger.setAspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mUVCCameraHandlerLarger = UVCCameraHandler.createHandler(getActivity(), mUVCCameraViewLarger,
                displayMetrics.widthPixels, displayMetrics.heightPixels);
        mUVCCameraHandlerLarger.setCanAjustViewSize(true);// 自动去取合适分辨率

        largerPreSize = new DisplayFragment.PrevSize();

        mGalleryButton = (zxImageView) view.findViewById(R.id.imageButtonGallery);
        mGalleryButton.setOnClickListener(this);
        mSwitchModeButton = (ImageButton) view.findViewById(R.id.imageButtonShutterMode);
        mSwitchModeButton.setOnClickListener(this);
        mShutterButton = (ImageButton) view.findViewById(R.id.imageButtonActionShutter);
        mShutterButton.setOnClickListener(this);

        mRecordingTimeView = (TextView) view.findViewById(R.id.recording_time);
        mTimeLapseLabel = view.findViewById(R.id.time_lapse_label);
        mLabelsLinearLayout = (LinearLayout) view.findViewById(R.id.labels);

        mUVCCameraViewLarger.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                largerPreSize.width = width;
                largerPreSize.height = height;
                if(mUVCCameraHandlerLarger != null) {
                    mUVCCameraHandlerLarger.setSize(width, height);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUSBMonitor.requestPermission(mDefaultDevice);
                    }
                });

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

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
//            Logger.getLogger().d("onAttach: " + device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
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
                    Toast.makeText(getActivity(),
                            (device.getProductName() != null) ? device.getProductName() : ""  + getString(R.string.device_dettach),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
//            Logger.getLogger().d("onConnect: " + device);

            if (!mUVCCameraHandlerLarger.isOpened()) {
                mUVCCameraHandlerLarger.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewLarger.getSurfaceTexture();
                mUVCCameraHandlerLarger.startPreview(new Surface(st));

            }
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            if ((mUVCCameraHandlerLarger != null) && !mUVCCameraHandlerLarger.isEqual(device)) {
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
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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

                    Intent intent = new Intent(getActivity(), GalleryActivityTv.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(GalleryActivityTv.KEY_INPUT_CHECKED_LIST_PATH, Storage.DIRECTORY);
                    bundle.putInt(GalleryActivityTv.KEY_INPUT_SELECT_INDEX, 0);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), R.string.no_data, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

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
                getActivity().sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO,
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
                mStorageHint = OnScreenHint.makeText(getActivity(), errorMessage);
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

    private void onShutterButtonClick() {
        Logger.getLogger().i("*******onShutterButtonClick***********mCurrrentMode " + mCurrrentMode);
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
            Logger.getLogger().i("*******onShutterButtonClick***********mCurrrentMode " + mCurrrentMode);
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
}
