package com.zx.tv.camera.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.zx.tv.camera.R;

public class zxImageButton extends ImageView implements
        View.OnTouchListener,
        View.OnLongClickListener {
    private static final String namespace = "http://schemas.android.com/apk/res-auto";

    private float mRotation;
    private float mRadius;
    private int mbkColor;
    private Bitmap mButBmp;
    private Bitmap mOverlayIcon;
    private float mOverlayIconX;
    private float mOverlayIconY;
    private int mPadding;
    private int mAction;

    private boolean misPressed;
    private ColorMatrixColorFilter mColorFilter;
    Paint mButPaint = new Paint();
    ValueAnimator mAnimation = ValueAnimator.ofFloat(1f, 0f);

    public zxImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRotation = 0;
        mAction = 0;
        mLongClickListener = null;

        mRadius = attrs.getAttributeIntValue(namespace, "roundRadius", 10);
        mbkColor = attrs.getAttributeIntValue(namespace, "bkColor", 0xFF69cadc);

        int resID = attrs.getAttributeResourceValue(namespace, "buttonImageId", R.drawable.grey);
        mButBmp = BitmapFactory.decodeResource(getResources(), resID);

        mPadding = attrs.getAttributeIntValue(namespace, "padding", 0);

        setOnTouchListener(this);
    }

    public void setButtonImage(Bitmap ButBmp) {
        mButBmp = ButBmp;
        invalidate();
    }

    public void setButtonImage(int resID) {
        mButBmp = BitmapFactory.decodeResource(getResources(), resID);
        invalidate();
    }

    public void setActionMode(int mode) {
        mAction = mode;
    }

    public void showOverlayIcon(int recID, float x, float y) {
        mOverlayIcon = BitmapFactory.decodeResource(getResources(), recID);
        mOverlayIconX = x;
        mOverlayIconY = y;
        invalidate();
    }

    public void clearOverlayIcon() {
        mOverlayIcon = null;
        invalidate();
    }

    public interface OnLongClickListener {
        void onLongClickStart(View v);

        void onLongClickStop(View v);
    }

    private OnLongClickListener mLongClickListener;
    private boolean mLongClick;

    public void setLongClickListener(OnLongClickListener listener) {
        mLongClickListener = listener;
        if (listener != null) {
            setOnLongClickListener(this);
            mLongClick = false;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mLongClickListener != null) {
            mLongClickListener.onLongClickStart(v);
            mLongClick = true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                misPressed = true;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                misPressed = false;

                if (mLongClick) {
                    mLongClick = false;
                    mLongClickListener.onLongClickStop(v);
                }

                invalidate();
                break;
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        mButPaint.setColor(mbkColor);

        Rect r = new Rect();
        getDrawingRect(r);
        r.inset(mPadding, mPadding);

        boolean showbk = true;
        if (mAction == 1)
            showbk = false;

        if (misPressed) {
            if (mAction == 0)
                r.offset(4, 4);
            else
                showbk = true;
        }

        RectF rf = new RectF(r);
//        if (showbk) {
//            canvas.drawRoundRect(rf, mRadius, mRadius, mButPaint);
//        }

        float
                x = (rf.width() - mButBmp.getWidth()) / 2 + rf.left,
                y = (rf.height() - mButBmp.getHeight()) / 2 + rf.top;

        canvas.drawBitmap(mButBmp, x, y, mButPaint);

        if (mOverlayIcon != null) {
            x = rf.left + mOverlayIconX;
            y = rf.top + mOverlayIconY;
            canvas.drawBitmap(mOverlayIcon, x, y, mButPaint);
        }
    }

    private void fade() {
        mAnimation.setDuration(800);
        mAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ColorMatrix matrix = new ColorMatrix();

                matrix.setSaturation((float) animation.getAnimatedValue());

                mColorFilter = new ColorMatrixColorFilter(matrix);
                mButPaint.setColorFilter(mColorFilter);
                postInvalidate();
            }
        });
        mAnimation.setStartDelay(200);
        mAnimation.start();
    }

    public void setRotation(float rotation) {
        if ((rotation - mRotation) > 180)
            rotation -= 360;

        if ((rotation - mRotation) < -180)
            rotation += 360;

        RotateAnimation animation;
        animation = new RotateAnimation(mRotation, rotation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setFillAfter(true);
        animation.setDuration(150);
        startAnimation(animation);

        mRotation = rotation;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            if (enabled) {
                mAnimation.cancel();
                mButPaint.setColorFilter(null);
                postInvalidate();
            } else
                fade();

            invalidate();
        }
        super.setEnabled(enabled);
    }

    public void disableForAWhile(int ms) {
        setEnabled(false);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setEnabled(true);
            }
        }, ms);
    }
}
