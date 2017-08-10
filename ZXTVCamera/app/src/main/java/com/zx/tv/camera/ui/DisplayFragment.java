package com.zx.tv.camera.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.zx.tv.camera.R;
import com.zx.tv.camera.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2017-08-04
 * Time: 10:25
 * Company: zx
 * Description:
 * FIXME
 */


public class DisplayFragment extends BaseFragment implements View.OnClickListener, CameraDialog.CameraDialogParent {

    private static final int START_PREVIEW_FIRST = 1;
    private static final int START_PREVIEW_SECOND = 2;
    private static final int START_PREVIEW_THIRD = 3;
    private static final int START_PREVIEW_FOURTH = 4;

    private static final int MAX_WINDOWS_NUM = 4;


    private static final float[] BANDWIDTH_FACTORS = {0.5F, 0.5F};


    private final Object mSync = new Object();

    private List<UsbDevice> mDeviceList;

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera; // 拍照 录像

    private View mDualCameraView;

    private View L2_mDualcameraView;

    private RelativeLayout mLeftCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerL;
    private CameraViewInterface mUVCCameraViewL;
    private TextView tvCameraPromateL;


    private RelativeLayout mRightCameraLayout;
    private UVCCameraHandler mUVCCameraHandlerR;
    private CameraViewInterface mUVCCameraViewR;
    private TextView tvCameraPromateR;

    private RelativeLayout L2_mLeftCameraLayout;
    private UVCCameraHandler L2_mUVCCameraHandlerL;
    private CameraViewInterface L2_mUVCCameraViewL;
    private TextView L2_tvCameraPromateL;

    private RelativeLayout L2_mRightCameraLayout;
    private UVCCameraHandler L2_mUVCCameraHandlerR;
    private CameraViewInterface L2_mUVCCameraViewR;
    private TextView L2_tvCameraPromateR;


    private PrevSize leftPrevSize;
    private PrevSize rightPreSize;

    private PrevSize L2_leftPrevSize;
    private PrevSize L2_rightPreSize;


    //    private SparseBooleanArray mDeviceConnected = new SparseBooleanArray();
    private SparseArray<String> mUsbDeviceName = new SparseArray<>();
    private HashMap<Integer, Integer> mDeviceConnectedMap = new HashMap<>();

    private SparseArray<WeakReference<UsbDevice>> mDeviceConnected = new SparseArray<>();

    private final Handler mHandler = new MainHandler();

    private boolean manaualOpen = false;
    private int manaualOpenWMID = -1;

    public DisplayFragment() {

    }

    public static DisplayFragment newInstance() {
        DisplayFragment displayFragment = new DisplayFragment();
        return displayFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.display_fragment, container, false);

        initView(view);
        Logger.getLogger().i("*************onCreateView***************");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.getLogger().i("*************onViewCreated***************");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUSBMonitor = new USBMonitor(getActivity(), mOnDeviceConnectListener);
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
        mDeviceList = mUSBMonitor.getDeviceList(filter.get(0));
        if (mDeviceList.size() == 0) {
            Toast.makeText(getActivity(), R.string.have_no_device, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.getLogger().i("*************onResume***************");
        mUSBMonitor.register();

        if (mDeviceList.isEmpty()) {
            Toast.makeText(getActivity(), R.string.have_no_device, Toast.LENGTH_SHORT).show();
        }
//
//        if (mUVCCameraViewL != null) {
//            mUVCCameraViewL.onResume();
//        }
//        if (mUVCCameraViewR != null) {
//            mUVCCameraViewR.onResume();
//        }
//
//        if (L2_mUVCCameraViewL != null) {
//            L2_mUVCCameraViewL.onResume();
//        }
//        if (L2_mUVCCameraViewR != null) {
//            L2_mUVCCameraViewR.onResume();
//        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.getLogger().i("*************onPause***************");
    }

    @Override
    public void onStop() {

        mUSBMonitor.unregister();
        Logger.getLogger().i("*************onStop***************");
        mUVCCameraHandlerL.close();
        if (mUVCCameraViewL != null) {
            mUVCCameraViewL.onPause();
        }

        mUVCCameraHandlerR.close();
        if (mUVCCameraViewR != null) {
            mUVCCameraViewR.onPause();
        }

        L2_mUVCCameraHandlerL.close();
        if (L2_mUVCCameraViewL != null) {
            L2_mUVCCameraViewL.onPause();
        }

        L2_mUVCCameraHandlerR.close();
        if (L2_mUVCCameraViewR != null) {
            L2_mUVCCameraViewR.onPause();
        }

        mDeviceList.clear();
        mDeviceConnected.clear();
        mUsbDeviceName.clear();

        super.onStop();
    }

    @Override
    public void onDestroy() {

        if (mUVCCameraHandlerL != null) {
            mUVCCameraHandlerL = null;
        }
        if (mUVCCameraHandlerR != null) {
            mUVCCameraHandlerR = null;
        }

        if (L2_mUVCCameraHandlerL != null) {
            L2_mUVCCameraHandlerL = null;
        }
        if (L2_mUVCCameraHandlerR != null) {
            L2_mUVCCameraHandlerR = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraViewL = null;
        mUVCCameraViewR = null;

        L2_mUVCCameraViewL = null;
        L2_mUVCCameraViewR = null;

        super.onDestroy();
    }

    private void initView(View rootView) {
        mDualCameraView = rootView.findViewById(R.id.dual_camera_view);
        L2_mDualcameraView = rootView.findViewById(R.id.dual_camera_view_level_2);

        //****************************************************************************

        mLeftCameraLayout = (RelativeLayout) rootView.findViewById(R.id.camera_layout_L);
        mLeftCameraLayout.setOnClickListener(this);

        mRightCameraLayout = (RelativeLayout) rootView.findViewById(R.id.camera_layout_R);
        mRightCameraLayout.setOnClickListener(this);

        L2_mLeftCameraLayout = (RelativeLayout) rootView.findViewById(R.id.L2_camera_layout_L);
        L2_mLeftCameraLayout.setOnClickListener(this);

        L2_mRightCameraLayout = (RelativeLayout) rootView.findViewById(R.id.L2_camera_layout_R);
        L2_mRightCameraLayout.setOnClickListener(this);

        //****************************************************************************

        mUVCCameraViewL = (CameraViewInterface) rootView.findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraHandlerL = UVCCameraHandler.createHandler(getActivity(), mUVCCameraViewL,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateL = (TextView) rootView.findViewById(R.id.camera_open_promate_L);
        leftPrevSize = new PrevSize();

        mUVCCameraViewR = (CameraViewInterface) rootView.findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraHandlerR = UVCCameraHandler.createHandler(getActivity(), mUVCCameraViewR,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        tvCameraPromateR = (TextView) rootView.findViewById(R.id.camera_open_promate_R);
        rightPreSize = new PrevSize();

        L2_mUVCCameraViewL = (CameraViewInterface) rootView.findViewById(R.id.L2_camera_view_L);
        L2_mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        L2_mUVCCameraHandlerL = UVCCameraHandler.createHandler(getActivity(), mUVCCameraViewL,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        L2_tvCameraPromateL = (TextView) rootView.findViewById(R.id.L2_camera_open_promate_L);
        L2_leftPrevSize = new PrevSize();

        L2_mUVCCameraViewR = (CameraViewInterface) rootView.findViewById(R.id.L2_camera_view_R);
        L2_mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        L2_mUVCCameraHandlerR = UVCCameraHandler.createHandler(getActivity(), mUVCCameraViewR,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        L2_tvCameraPromateR = (TextView) rootView.findViewById(R.id.L2_camera_open_promate_R);
        L2_rightPreSize = new PrevSize();


        mUVCCameraHandlerL.setCanAjustViewSize(true);// 自动去取合适分辨率
        mUVCCameraHandlerR.setCanAjustViewSize(true);// 自动去取合适分辨率
        addSurfceCallback();

    }

    private void addSurfceCallback() {
        mUVCCameraViewL.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {

                leftPrevSize.width = width;
                leftPrevSize.height = height;
                if(mUVCCameraHandlerL != null) {
//                    mUVCCameraViewL.setAspectRatio(width / height);
                    mUVCCameraHandlerL.setSize(width, height);
                }

                try {
                    final UsbDevice device = mDeviceList.get(0);
                    if (null != device) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mSync) {
                                    mUSBMonitor.requestPermission(device);
                                }
                                Logger.getLogger().i("************onSurfaceCreated*********** connected");
                            }
                        });

                        mDeviceConnectedMap.put(USBMonitor.getDeviceKey(device, true), 0);
                    }
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger().i("index 0 device " + e.getMessage());
                }


                Logger.getLogger().i("width = " + width + "********** heigt = " + height);
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
                Logger.getLogger().i("************onSurfaceCreated*********** 1");
                try {
                    final UsbDevice device = mDeviceList.get(1);
                    if (null != device) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mSync) {
                                    mUSBMonitor.requestPermission(device);
                                }
                            }
                        });
                    }
                    mDeviceConnectedMap.put(USBMonitor.getDeviceKey(device, true), 1);
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger().i("index 1 device " + e.getMessage());
                }

            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {

            }
        });
        L2_mUVCCameraViewL.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                try {
                    final UsbDevice device = mDeviceList.get(2);
                    if (null != device) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mSync) {
                                    mUSBMonitor.requestPermission(device);
                                }
                            }
                        });
                    }

                    mDeviceConnectedMap.put(USBMonitor.getDeviceKey(device, true), 2);
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger().i("index 2 device " + e.getMessage());
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {

            }
        });
        L2_mUVCCameraViewR.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface, int width, int height) {
                try {
                    final UsbDevice device = mDeviceList.get(3);
                    if (null != device) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mSync) {
                                    mUSBMonitor.requestPermission(device);
                                }
                            }
                        });
                    }
                    mDeviceConnectedMap.put(USBMonitor.getDeviceKey(device, true), 3);
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger().i("index 3 device " + e.getMessage());
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {

            }
        });
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Logger.getLogger().i("*********** onAttach***********");
            if (device.getProductName() != null && (device.getProductName().contains("wlan")
                    || device.getProductName().contains("WLAN"))) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
                            (device.getProductName() != null) ? device.getProductName() : "" + getString(R.string.device_attach),
                            Toast.LENGTH_SHORT).show();
                }
            });

            int deviceKey = USBMonitor.getDeviceKey(device, true);
            WeakReference<UsbDevice> deviceWeakReference = mDeviceConnected.get(deviceKey);
            if (deviceWeakReference == null || (deviceWeakReference == null && deviceWeakReference.get() == null)) {
                Logger.getLogger().i("*********** onAttach*******requestPermission****" + deviceKey);
                synchronized (mSync) {

                    mUSBMonitor.requestPermission(device);
                }

            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Logger.getLogger().d("onDettach: " + device.getProductName());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
                            (device.getProductName() != null) ? device.getProductName() : "" + getString(R.string.device_dettach),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Logger.getLogger().d("***************onConnect: " +
                    (device.getProductName() != null ? device.getProductName() : " " + device.getDeviceName()));
            int deviceKey = USBMonitor.getDeviceKey(device, true);
            WeakReference<UsbDevice> deviceWeakReference = mDeviceConnected.get(deviceKey);
            if (deviceWeakReference != null && deviceWeakReference.get() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), R.string.device_conneted_warn,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            int windowId = -1;
            Integer valueObj = mDeviceConnectedMap.get(deviceKey);
            if (valueObj == null) {
                windowId = findFreeWindow();
                mDeviceConnectedMap.put(deviceKey, windowId);
            } else {
                windowId = valueObj;
            }

            if (!mUVCCameraHandlerL.isOpened()
                    && (windowId == 0 || (manaualOpen == true && manaualOpenWMID == 0))) {

                connectDevice(device, ctrlBlock, 0);

            } else if (!mUVCCameraHandlerR.isOpened()
                    && (windowId == 1 || (manaualOpen == true && manaualOpenWMID == 0))) {

                connectDevice(device, ctrlBlock, 1);
            } else if (!L2_mUVCCameraHandlerL.isOpened()
                    && (windowId == 2 || (manaualOpen == true && manaualOpenWMID == 0))) {

                connectDevice(device, ctrlBlock, 2);
            } else if (!L2_mUVCCameraHandlerR.isOpened()
                    && (windowId == 3 || (manaualOpen == true && manaualOpenWMID == 0))) {

                connectDevice(device, ctrlBlock, 3);
            }

        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Logger.getLogger().d("**************onDisconnect: " + device.getProductName());
            if (device.getProductName() != null && (device.getProductName().contains("wlan")
                    || device.getProductName().contains("WLAN"))) {
                return;
            }

            int deviceKey = USBMonitor.getDeviceKey(device, true);
            Integer integerIndex = mDeviceConnectedMap.get(deviceKey);
            int index = integerIndex == null ? -1 : integerIndex;
            Logger.getLogger().d("****************onDisconnect: " + mDeviceConnectedMap.get(deviceKey));
            if ((mUVCCameraHandlerL != null) && 0 == index) {

                disconnectDevice(device, mUVCCameraHandlerL, 1, tvCameraPromateL);
            } else if ((mUVCCameraHandlerR != null) && 1 == index) {

                disconnectDevice(device, mUVCCameraHandlerR, 1, tvCameraPromateR);
            } else if ((L2_mUVCCameraHandlerL != null) && 2 == index) {

                disconnectDevice(device, L2_mUVCCameraHandlerL, 2, L2_tvCameraPromateL);
            } else if ((L2_mUVCCameraHandlerR != null) && 3 == index) {

                disconnectDevice(device, L2_mUVCCameraHandlerR, 3, L2_tvCameraPromateR);
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            Logger.getLogger().d("onCancel: " + device);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_layout_L:
                if (mUVCCameraHandlerL != null) {
                    if (!mUVCCameraHandlerL.isOpened()) {
                        CameraDialog.showDialog(getActivity(), mUSBMonitor);
                        manaualOpen = true;
                        manaualOpenWMID = 0;
                    } else {
                        mUVCCameraHandlerL.close();
                        openCaeralLarger(0);

                    }
                }
                break;
            case R.id.camera_layout_R:
                if (mUVCCameraHandlerR != null) {
                    if (!mUVCCameraHandlerR.isOpened()) {
                        CameraDialog.showDialog(getActivity(), mUSBMonitor);
                        manaualOpen = true;
                        manaualOpenWMID = 1;
                    } else {
                        mUVCCameraHandlerR.close();
                        openCaeralLarger(1);
                    }
                }
                break;
            case R.id.L2_camera_layout_L:
                if (L2_mUVCCameraHandlerR != null) {
                    if (!L2_mUVCCameraHandlerR.isOpened()) {
                        CameraDialog.showDialog(getActivity(), mUSBMonitor);
                        manaualOpen = true;
                        manaualOpenWMID = 2;
                    } else {
                        mUVCCameraHandlerR.close();
                        openCaeralLarger(2);
                    }
                }
                break;
            case R.id.L2_camera_layout_R:
                if (L2_mUVCCameraHandlerR != null) {
                    if (!L2_mUVCCameraHandlerR.isOpened()) {
                        CameraDialog.showDialog(getActivity(), mUSBMonitor);
                        manaualOpen = true;
                        manaualOpenWMID = 3;
                    } else {
                        mUVCCameraHandlerR.close();
                        openCaeralLarger(3);
                    }
                }
                break;

        }
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {

    }

    private int findFreeWindow() {
        int windowId = -1;
        for (int index = 0; index < MAX_WINDOWS_NUM; index++) {
            if (mDeviceConnectedMap.containsValue(index) == false) {
                windowId = index;
                break;
            }
        }
        return windowId;
    }

    private void connectDevice(final UsbDevice device, final USBMonitor.UsbControlBlock controlBlock, final int windowId) {
        UVCCameraHandler handler = null;
        CameraViewInterface cameraViewInterface = null;
        TextView connectInfo = null;
        switch (windowId) {
            case 0:
                handler = mUVCCameraHandlerL;
                cameraViewInterface = mUVCCameraViewL;
                connectInfo = tvCameraPromateL;
                break;
            case 1:
                handler = mUVCCameraHandlerR;
                cameraViewInterface = mUVCCameraViewR;
                connectInfo = tvCameraPromateR;
                break;
            case 2:
                handler = L2_mUVCCameraHandlerL;
                cameraViewInterface = L2_mUVCCameraViewL;
                connectInfo = L2_tvCameraPromateL;
                break;
            case 3:
                handler = L2_mUVCCameraHandlerR;
                cameraViewInterface = L2_mUVCCameraViewR;
                connectInfo = L2_tvCameraPromateR;
                break;
        }
        if (handler.isOpened() || cameraViewInterface == null) {
            return;
        }
        int deviceKey = USBMonitor.getDeviceKey(device, true);
        handler.open(controlBlock);
        final SurfaceTexture st = cameraViewInterface.getSurfaceTexture();
        handler.startPreview(new Surface(st));
        final View tvInfo = connectInfo;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvInfo.setVisibility(View.GONE);
            }
        });

        String deviceName = String.format("UVC Camera:(%x:%x:%s)", device.getVendorId(), device.getProductId(), device.getDeviceName());
        mUsbDeviceName.put(windowId, deviceName);
        mDeviceConnected.put(USBMonitor.getDeviceKey(device, true), new WeakReference<UsbDevice>(device));
        mDeviceConnectedMap.put(deviceKey, windowId);

        manaualOpen = false;
        manaualOpenWMID = -1;
    }

    private void disconnectDevice(final UsbDevice device, final UVCCameraHandler handler,
                                  final int windowId, final TextView connectInfo) {
        int deviceKey = USBMonitor.getDeviceKey(device, true);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                handler.close();
            }
        }, 0);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectInfo.setVisibility(View.VISIBLE);
                connectInfo.setText(R.string.device_open_error);
            }
        });

//        mUsbDeviceName.delete(windowId);
        mDeviceConnected.remove(USBMonitor.getDeviceKey(device, true));
        mDeviceConnectedMap.remove(deviceKey);
    }


    private class MainHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case START_PREVIEW_FIRST:
//                    break;
//                case START_PREVIEW_FIRST:
//                    break;
//                case START_PREVIEW_FIRST:
//                    break;
//                case START_PREVIEW_FIRST:
//                    break;

            }
        }
    }

    private void openCaeralLarger(int cameraId) {
        PreviewFragment fragment = PreviewFragment.newInstance();
        Bundle bundle = new Bundle();
        String deviceName = mUsbDeviceName.get(cameraId);
        Logger.getLogger().i("**************deviceName " + deviceName);
        bundle.putString("devicename", deviceName);
        fragment.setArguments(bundle);
        FragmentManager fm = getActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container, fragment, "preview");
        ft.addToBackStack(null);
        ft.commit();
    }

    public static class PrevSize {
        public int width;
        public int height;
    }
}
