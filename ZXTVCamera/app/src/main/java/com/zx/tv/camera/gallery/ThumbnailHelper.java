package com.zx.tv.camera.gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

/**
 * Created by BettySong on 12/17/15.
 */
public class ThumbnailHelper {
    private static final String TAG = "com.zhaoxin.zxcamera.gallery.ThumbnailHelper";

    public static Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //first get the width and height of the pic,and set bitmap as null to save memory
        BitmapFactory.decodeFile(imagePath, options);
        //calculate scale
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        options.inJustDecodeBounds = false;
        //read picture again but at scaled size.
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        //create thumbnail
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /*
    ** @param kind could be Images.Thumbnails.MINI_KIND or Images.Thumbnails.MICRO_KIND
    */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                           int kind) {
        Bitmap bitmap = null;
        //get video thubmail
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        if (bitmap == null)
            return bitmap;
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /*
    ** @param kind could be Images.Thumbnails.MINI_KIND or Images.Thumbnails.MICRO_KIND
    */
    public static Bitmap getVideoThumbnail(String videoPath, int kind) {
        Bitmap bitmap = null;
        //get video thubmail
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);

        return bitmap;
    }

}
