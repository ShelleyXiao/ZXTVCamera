package com.zx.tv.camera.gallery;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Gallery;

import com.zx.tv.camera.R;
import com.zx.tv.camera.utils.Logger;


public class PicGallery extends Gallery {
    private GestureDetector mGestureScanner;
    private GalleryImageView mImageView;
    private int mScreenWidth;
    private int mScreenHeight;
    public static final int KEY_INVALID = -1;
    private int kEvent = KEY_INVALID; //invalid

    public PicGallery(Context context) {
        super(context);
    }

    public PicGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDetector(GestureDetector detector) {
        mGestureScanner = detector;
    }

    public PicGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnTouchListener(new OnTouchListener() {
            float baseValue;
            float originalScale;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                View view = PicGallery.this.getSelectedView().findViewById(R.id.bitmap);

                if (view instanceof GalleryImageView) {
                    mImageView = (GalleryImageView) view;

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        baseValue = 0;
                        originalScale = mImageView.getScale();
                    }
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (event.getPointerCount() == 2) {
                            float x = event.getX(0) - event.getX(1);
                            float y = event.getY(0) - event.getY(1);
                            float value = (float) Math.sqrt(x * x + y * y);// comptute the distance between two point
                            // System.out.println("value:" + value);
                            if (baseValue == 0) {
                                baseValue = value;
                            } else {
                                float scale = value / baseValue;// current distance between two points divide the distance of two point when figner fist touch screen.
                                // scale the image
                                mImageView.zoomTo(originalScale * scale, x
                                        + event.getX(1), y + event.getY(1));
                            }
                        }
                    }
                }
                return false;
            }

        });
    }

    float v[] = new float[9];

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        View view = PicGallery.this.getSelectedView().findViewById(R.id.bitmap);
        if (view instanceof GalleryImageView) {
            float xdistance = calXdistance(e1, e2);
            float min_distance = mScreenWidth / 8f;
            Logger.getLogger().d( "xdistance=" + xdistance + ",min_distance=" + min_distance);
            if (isScrollingLeft(e1, e2) && xdistance > min_distance) {
                kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
            } else if (!isScrollingLeft(e1, e2) && xdistance > min_distance) {
                kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
            }

            mImageView = (GalleryImageView) view;

            Matrix m = mImageView.getImageMatrix();
            m.getValues(v);
            float left, right;
            float width = mImageView.getScale() * mImageView.getImageWidth();
            float height = mImageView.getScale() * mImageView.getImageHeight();

            if ((int) width <= mScreenWidth
                    && (int) height <= mScreenWidth)// if size of picture < size of screen ,do scroll directly
            {
                super.onScroll(e1, e2, distanceX, distanceY);
            } else {
                left = v[Matrix.MTRANS_X];
                right = left + width;
                Rect r = new Rect();
                mImageView.getGlobalVisibleRect(r);

                if (distanceX > 0)// scroll to left
                {
                    if (r.left > 0 || right < mScreenWidth) {// judge whether show the current picture of not
                        super.onScroll(e1, e2, distanceX, distanceY);
                    } else {
                        mImageView.postTranslate(-distanceX, -distanceY);
                    }
                } else if (distanceX < 0)//scroll to right
                {
                    if (r.right < mScreenWidth || left > 0) {
                        super.onScroll(e1, e2, distanceX, distanceY);
                    } else {
                        mImageView.postTranslate(-distanceX, -distanceY);
                    }
                }
            }

        } else {
            super.onScroll(e1, e2, distanceX, distanceY);
        }

        return false;
    }

    private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2) {
        return e2.getX() > e1.getX();
    }

    private float calXdistance(MotionEvent e1, MotionEvent e2) {
        return Math.abs(e2.getX() - e1.getX());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Logger.d(DEBUG,"[PicGallery.onTouchEvent]"+"PicGallery.onTouchEvent");
        if (mGestureScanner != null) {
            mGestureScanner.onTouchEvent(event);
        }
        View view = PicGallery.this.getSelectedView().findViewById(R.id.bitmap);
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (view instanceof GalleryImageView) {
                    if (kEvent != KEY_INVALID) { // whether switch to previous page or the next page
                        onKeyDown(kEvent, null);
                        kEvent = KEY_INVALID;
                    }

                    mImageView = (GalleryImageView) view;
                    float width = mImageView.getScale() * mImageView.getImageWidth();
                    float height = mImageView.getScale() * mImageView.getImageHeight();
                    // Logger.LOG("onTouchEvent", "width=" + width + ",height="
                    // + height + ",screenWidth="
                    // + GalleryMainActivity.screenWidth + ",screenHeight="
                    // + GalleryMainActivity.screenHeight);
                    if ((int) width <= mScreenWidth
                            && (int) height <= mScreenWidth) {
                        break;
                    }
                    float v[] = new float[9];
                    Matrix m = mImageView.getImageMatrix();
                    m.getValues(v);
                    float top = v[Matrix.MTRANS_Y];
                    float bottom = top + height;
                    if (top < 0 && bottom < mScreenWidth) {
                        //  imageView.postTranslateDur(-top, 200f);
                        mImageView.postTranslateDur(mScreenWidth
                                - bottom, 200f);
                    }
                    if (top > 0 && bottom > mScreenWidth) {
                        //  imageView.postTranslateDur(GalleryMainActivity.screenHeight
                        //  - bottom, 200f);
                        mImageView.postTranslateDur(-top, 200f);
                    }

                    float left = v[Matrix.MTRANS_X];
                    float right = left + width;
                    if (left < 0 && right < mScreenWidth) {
                        //  imageView.postTranslateXDur(-left, 200f);
                        mImageView.postTranslateXDur(mScreenWidth
                                - right, 200f);
                    }
                    if (left > 0 && right > mScreenWidth) {
                        //  imageView.postTranslateXDur(GalleryMainActivity.screenWidth
                        //  - right, 200f);
                        mImageView.postTranslateXDur(-left, 200f);
                    }
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    public void setScreenSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }

}
