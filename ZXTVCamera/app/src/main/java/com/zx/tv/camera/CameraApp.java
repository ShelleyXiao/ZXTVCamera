package com.zx.tv.camera;

import android.app.Application;

import com.zx.album.Album;
import com.zx.album.AlbumConfig;
import com.zx.album.task.LocalImageLoader;
import com.zx.tv.camera.utils.Util;

import java.util.Locale;

/**
 * User: ShaudXiao
 * Date: 2017-06-07
 * Time: 14:55
 * Company: zx
 * Description:
 * FIXME
 */


public class CameraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Util.initlialize(this);

        Album.initialize(new AlbumConfig.Build()
                .setImageLoader(new LocalImageLoader())
                .setLocale(Locale.getDefault())
                .build()
        );

    }
}
