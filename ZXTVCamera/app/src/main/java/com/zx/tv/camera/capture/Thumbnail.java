package com.zx.tv.camera.capture;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.zx.tv.camera.utils.Logger;
import com.zx.tv.camera.utils.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * User: ShaudXiao
 * Date: 2017-06-07
 * Time: 13:49
 * Company: zx
 * Description:
 * FIXME
 */


public class Thumbnail {

    private static final String LAST_THUMB_FILENAME = "last_thumbnail";
    private static final int BUFSIZE = 4096;

    private Uri mUri;
    private Bitmap mBitmap;

    private boolean mFromFile = false;

    private static Object sLock = new Object();

    public Thumbnail(Uri uri, Bitmap bitmap) {
        mUri = uri;
        mBitmap = bitmap;
        if (null == mBitmap) {
            throw new IllegalArgumentException("null bitmap");
        }
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public boolean isFromFile() {
        return mFromFile;
    }

    public void setFromFile(boolean fromFile) {
        mFromFile = fromFile;
    }

    public void saveTo(File file) {
        FileOutputStream f = null;
        BufferedOutputStream b = null;
        DataOutputStream d = null;
        synchronized (sLock) {
            try {
                f = new FileOutputStream(file);
                b = new BufferedOutputStream(f, BUFSIZE);
                d = new DataOutputStream(b);
                d.writeUTF(mUri.toString());
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, d);
                d.close();
            } catch (IOException e) {
                Logger.getLogger().e("Fail to store bitmap . path = " + file.getPath());
            } finally {
                Util.closeSilently(f);
                Util.closeSilently(b);
                Util.closeSilently(d);
            }
        }
    }

    public static Thumbnail loadFrom(File file) {
        Uri uri = null;
        Bitmap bitmap = null;
        FileInputStream f = null;
        BufferedInputStream b = null;
        DataInputStream d = null;
        synchronized (sLock) {
            try {
                f = new FileInputStream(file);
                b = new BufferedInputStream(f, BUFSIZE);
                d = new DataInputStream(b);
                uri = Uri.parse(d.readUTF());
                bitmap = BitmapFactory.decodeStream(d);
                d.close();
            } catch (IOException e) {
                Logger.getLogger().e("Fail to load bitmap . path = " + e);
            } finally {
                Util.closeSilently(f);
                Util.closeSilently(b);
                Util.closeSilently(d);
            }
        }
        Thumbnail thumbnail = crateThumbnail(uri, bitmap);
        if (null != thumbnail) {
            thumbnail.setFromFile(true);
        }

        return thumbnail;

    }

    public static Thumbnail getLastThumbnail(ContentResolver resolver) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if(image == null && video == null) {
            return  null;
        }

        Bitmap bitmap = null;
        Media lastMedia;
        if(image != null &&(video != null || image.dateTaken >= video.dateTaken)) {
            bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, Images.Thumbnails.MINI_KIND, null);
            lastMedia = image;
        } else {
            bitmap = Video.Thumbnails.getThumbnail(resolver, video.id,
                    Video.Thumbnails.MINI_KIND, null);
            lastMedia = video;
        }

        if(Util.isUriVaild(lastMedia.uri, resolver)) {
            return crateThumbnail(lastMedia.uri, bitmap);
        }

        return null;

    }

    public static Media getLastImageThumbnail(ContentResolver resolver) {
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;

        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[] {
                ImageColumns._ID,
                ImageColumns.ORIENTATION,
                ImageColumns.DATE_TAKEN
        };
        String selection = ImageColumns.MIME_TYPE + "='image/jpeg' AND "
                + ImageColumns.BUCKET_ID + '=' + Storage.BUCKET_ID;
        String order = ImageColumns.DATE_TAKEN + " DESC,"
                + ImageColumns._ID + " DESC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, selection, null, order);
            if(null != cursor && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return new Media(id, cursor.getInt(1), cursor.getLong(2),
                        ContentUris.withAppendedId(baseUri, id));
            }
        } finally {
            if(null != cursor) {
                cursor.close();
            }
        }

        return null;

    }

    public static Media getLastVideoThumbnail(ContentResolver resolver) {
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;

        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[] {
                VideoColumns._ID,
                VideoColumns.DATA,
                VideoColumns.DATE_TAKEN
        };
        String selection = VideoColumns.BUCKET_ID + '=' + Storage.BUCKET_ID;
        String order = VideoColumns.DATE_TAKEN + " DESC,"
                + VideoColumns._ID + " DESC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, selection, null, order);
            if(null != cursor && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return new Media(id, cursor.getInt(1), cursor.getLong(2),
                        ContentUris.withAppendedId(baseUri, id));
            }
        } finally {
            if(null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    public static Thumbnail createThumbnail(byte[] jpeg, int orientation, int inSampleSize, Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
        return crateThumbnail(uri, bitmap);
    }

    private static Thumbnail crateThumbnail(Uri uri, Bitmap bitmap) {
        if (null == bitmap) {
            Logger.getLogger().e("Fialed to create thumbnail from null bitmap");
            return null;
        }
        try {
            return new Thumbnail(uri, bitmap);
        } catch (IllegalArgumentException e) {
            Logger.getLogger().e("Failed to construct thumbnail" + e);
            return null;
        }
    }

    private static class Media {
        public Media(long id, int orientation, long dateTaken, Uri uri) {
            this.id = id;
            this.orientation = orientation;
            this.dateTaken = dateTaken;
            this.uri = uri;
        }

        public final long id;
        public final int orientation;
        public final long dateTaken;
        public final Uri uri;
    }
}
