package com.zx.tv.camera.gallery;

import android.util.Log;

import com.zx.tv.camera.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 * Created by BettySong on 12/15/15.
 */
public class FileHelper {
    private static final String TAG = "com.zhaoxin.zxcamera.gallery.FileHelper";

    public static Boolean moveFile(String srcFileName, String dstDirName) {
        File srcFile = new File(srcFileName);
        if (!srcFile.exists() || !srcFile.isFile())
            return false;

        File dstDir = new File(dstDirName);
        if (!dstDir.exists())
            dstDir.mkdir();

        String dst = dstDirName + File.separator + srcFile.getName();

        return srcFile.renameTo(new File(dst));

    }

    public static Boolean deleteFile(String path) {
        Boolean res = false;
        File file = new File(path);

        if (file.exists())
            res = file.delete();
        else
            res = false;
        return res;
    }

    public static Boolean deleteFiles(String path) {
        File folder = new File(path);
        if (!folder.exists())
            return false;

        File[] childFiles = folder.listFiles();
        if (childFiles == null || childFiles.length == 0) {
            return folder.delete();
        }

        for (int i = 0; i < childFiles.length; i++) {
            if (!childFiles[i].delete())
                return false;
        }

        return folder.delete();
    }

    /**
     * @param filePath
     * @return file size with auto unit such like GB,MB,KB,B
     */
    public static String getAutoFileOrFilesSize(String filePath) {
        File file = new File(filePath);
        long blockSize = 0;
        try {
            if (file.isDirectory()) {
                blockSize = getFileSizes(file);
            } else {
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger().d( "get file size error!!!");
        }
        return FormatFileSize(blockSize);
    }

    private static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            file.createNewFile();
            Logger.getLogger().d( "File not exist!!!");
        }
        return size;
    }


    private static long getFileSizes(File f) throws Exception {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSizes(flist[i]);
            } else {
                size = size + getFileSize(flist[i]);
            }
        }
        return size;
    }

    /**
     * @param fileS
     */
    private static String FormatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }
}
