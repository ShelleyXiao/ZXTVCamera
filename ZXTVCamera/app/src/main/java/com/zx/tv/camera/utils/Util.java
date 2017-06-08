package com.zx.tv.camera.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.Closeable;
import java.io.IOException;

/**
 * User: ShaudXiao
 * Date: 2017-06-07
 * Time: 13:57
 * Company: zx
 * Description:
 * FIXME
 */


public class Util {

    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";

    // Private intent extras. Test only.
    private static final String EXTRAS_CAMERA_FACING =
            "android.intent.extras.CAMERA_FACING";
    private static ImageFileNamer sImageFileNamer;
    private static float sPixelDensity = 1;

    private Util() {
    }

    public static void initlialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
//        sImageFileNamer = new ImageFileNamer(
//                context.getString(R.string.image_file_name_format));
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {

        }
    }

    public static boolean isUriVaild(Uri uri, ContentResolver resolver) {
        if (null == uri) return false;

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if(pfd == null) {
                Logger.getLogger().e("Fail to open URI, URI=" + uri);
                return  false;
            }
            pfd.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static String createJpegName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateName(dateTaken);
        }
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_PICTURE, uri));
        // Keep compatibility
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static void showError(final Activity activity, int msgId) {
        DialogInterface.OnClickListener buttOnClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        };

//        new AlertDialog.Builder(activity)
//                .setCancelable(false)
//                .setIcon(android.R.attr.alertDialogIcon)
//                .setTitle(R.string.camera_error_title)
//                .setMessage(msgId)
//                .setNeutralButton(R.string.dialog_ok, buttOnClickListener)
//                .show();

    }

    public static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }

    public static String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    public static String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }

}
