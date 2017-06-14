package com.zx.tv.camera.gallery;

import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class ExifInfo {
    private static final String TAG = "com.zhaoxin.zxcamera.gallery.ExifInfo";

    private String mPath;
    private File mFile;
    private ExifInterface mExif;

    ExifInfo(String path) throws IOException {
        mExif = new ExifInterface(path);
        mFile = new File(path);
        mPath = path;
    }

    public String getDateTime() {
        return mExif.getAttribute(ExifInterface.TAG_DATETIME);
    }

    public String getImageWidth() {
        return mExif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
    }

    public String getImageHeight() {
        return mExif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
    }

    public String getImageOrientation() {
        return mExif.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    public String getFileSize() {
        return FileHelper.getAutoFileOrFilesSize(mPath);
    }

    public String getMaker() {
        return mExif.getAttribute(ExifInterface.TAG_MAKE);
    }

    public String getModel() {
        return mExif.getAttribute(ExifInterface.TAG_MODEL);
    }

    public String getFlash() {
        return mExif.getAttribute(ExifInterface.TAG_FLASH);
    }

    public String getFocalLength() {
        return mExif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
    }

    public String getWhiteBalance() {
        return mExif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
    }

    public String getAperture() {
        return mExif.getAttribute(ExifInterface.TAG_APERTURE);
    }

    public String getExposureTime() {
        return mExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
    }

    public String getIso() {
        return mExif.getAttribute(ExifInterface.TAG_ISO);
    }

    public String getTitle() {
        return mFile.getName();
    }

    public String getPath() {
        return mFile.getPath();
    }

    public String getHDR() {
        // TODO: get HDR info
        return null;
    }
}
