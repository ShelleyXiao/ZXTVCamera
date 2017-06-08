package com.zx.tv.camera;

import android.app.Application;

import com.zx.tv.camera.utils.Util;

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
    }
}
