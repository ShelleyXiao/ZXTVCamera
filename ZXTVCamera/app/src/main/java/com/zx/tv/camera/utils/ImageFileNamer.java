package com.zx.tv.camera.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: ShaudXiao
 * Date: 2017-06-07
 * Time: 14:50
 * Company: zx
 * Description:
 * FIXME
 */


public class ImageFileNamer {
    private SimpleDateFormat mFormat;
    private long mlastDate;
    private int mSameSecondCount;

    public ImageFileNamer(String format) {
        mFormat = new SimpleDateFormat(format);
    }

    public String generateName(long dateToken) {
        Date date = new Date(dateToken);
        String result = mFormat.format(date);

        if(dateToken / 1000 == mlastDate / 1000) {
            mSameSecondCount++;
            result += "_" + mSameSecondCount;
        } else {
            mlastDate = dateToken;
            mSameSecondCount = 0;
        }

        return result;
    }
}
