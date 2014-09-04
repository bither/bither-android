/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.ImageManageUtil;


public class DialogCropPhotoTransit extends Dialog {

    private static final int MaxShowingTime = 5000;

    private Context mContext;
    private Window mWindow;
    private WindowManager.LayoutParams mWindowLp;
    private ImageView mIv;
    private FrameLayout mFl;
    private LayoutParams ivLp;

    private Rect mFromRect;

    private Rect mToRect;

    private AnimationSet inAnim;

    private AnimationSet outAnim;

    private boolean inAnimFinished = false;
    private boolean isAnimationStarted = false;

    private final static int screenWidth = ImageManageUtil.getScreenWidth();

    private final static int screenHeight = BitherApplication.mContext
            .getResources().getDisplayMetrics().heightPixels;

    private final static int imageSize = screenWidth / 2;

    private float pauseScaleFactor;
    private float pauseLeft;
    private float pauseTop;

    private View finalImage;
    private ImageView finalImageView;
    private Runnable postRun = null;
    private boolean toShowAnimation;

    private Bitmap bitmap;

    private static DialogCropPhotoTransit instance = new DialogCropPhotoTransit(
            BitherApplication.mContext);

    public static DialogCropPhotoTransit getInstance() {
        return instance;
    }

    private DialogCropPhotoTransit(Context context) {
        super(context, R.style.dialogCropPhotoTransit);
        toShowAnimation = true;
        this.setCancelable(false);
        this.mContext = context;
        this.mWindow = this.getWindow();
        this.mWindowLp = mWindow.getAttributes();
        mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mWindowLp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowLp.windowAnimations = R.style.dialogCropPhotoTransit;
        mWindow.setAttributes(mWindowLp);
        this.setContentView(R.layout.dialog_crop_image_transit);
        this.mIv = (ImageView) findViewById(R.id.iv_photo);
        ivLp = (LayoutParams) mIv.getLayoutParams();
        mFl = (FrameLayout) findViewById(R.id.fl_container);
        mFl.getLayoutParams().height = mContext.getResources()
                .getDisplayMetrics().heightPixels;
        mFl.getLayoutParams().width = mContext.getResources()
                .getDisplayMetrics().widthPixels;
    }

    public void setFromRect(Rect rect) {
        this.mFromRect = rect;
    }

    public Rect getFromRect() {
        return this.mFromRect;
    }

    public void setToShowAnimation(boolean toShowAnimation) {
        this.toShowAnimation = toShowAnimation;
    }

    public void setToRect(Rect rect) {
        this.mToRect = rect;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            this.bitmap = bitmap;
            this.mIv.setImageBitmap(bitmap);
        }
    }

    private void initOutAnim() {
        outAnim = new AnimationSet(true);
        int imageWidth = mToRect.right - mToRect.left;
        int imageHeight = mToRect.bottom - mToRect.top;
        float scaleFactor = (float) Math.min(imageWidth, imageHeight)
                / (float) Math.min(mFromRect.right - mFromRect.left,
                mFromRect.bottom - mFromRect.top);
        ScaleAnimation scaleAnim = new ScaleAnimation(pauseScaleFactor,
                scaleFactor, pauseScaleFactor, scaleFactor, 0.5f, 0.5f);
        TranslateAnimation transAnim = new TranslateAnimation(pauseLeft,
                mToRect.left - mFromRect.left, pauseTop, mToRect.top
                - mFromRect.top
        );
        outAnim.addAnimation(scaleAnim);
        outAnim.addAnimation(transAnim);
        outAnim.setDuration(500);
        outAnim.setFillAfter(true);
        outAnim.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                mIv.setImageBitmap(null);
                if (finalImageView != null) {
                    finalImageView.setImageBitmap(bitmap);
                    if (postRun != null) {
                        finalImageView.postDelayed(postRun, 50);
                    }
                }
                if (finalImage != null) {
                    finalImage.setVisibility(View.VISIBLE);
                }
                dismiss();
            }
        });
    }

    private void initInAnim() {
        inAnim = new AnimationSet(true);
        int imageWidth = mFromRect.right - mFromRect.left;
        int imageHeight = mFromRect.bottom - mFromRect.top;
        pauseScaleFactor = (float) imageSize
                / (float) Math.min(imageWidth, imageHeight);
        ScaleAnimation scaleAnim = new ScaleAnimation(1, pauseScaleFactor, 1,
                pauseScaleFactor, 0.5f, 0.5f);
        pauseLeft = (screenWidth - imageWidth * pauseScaleFactor) / 2
                - mFromRect.left;
        pauseTop = (screenHeight - imageHeight * pauseScaleFactor) / 2
                - mFromRect.top;
        TranslateAnimation transAnim = new TranslateAnimation(0, pauseLeft, 0,
                pauseTop);
        inAnim.addAnimation(scaleAnim);
        inAnim.addAnimation(transAnim);
        inAnim.setDuration(500);
        inAnim.setFillAfter(true);
        inAnim.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
                inAnimFinished = false;
                isAnimationStarted = true;
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                inAnimFinished = true;
            }
        });
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void emptyBitmap() {
        bitmap = null;
        mIv.setImageBitmap(null);
    }

    public void Out(Rect outRect, View finalImage, ImageView imageView) {
        this.finalImage = finalImage;
        this.finalImageView = imageView;
        this.postRun = null;
        this.setToRect(outRect);
        initOutAnim();
        new Thread(new Runnable() {
            public void run() {
                while (!inAnimFinished) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mIv.post(new Runnable() {
                    public void run() {
                        mIv.startAnimation(outAnim);
                    }
                });
            }
        }).start();
    }

    public void Out(Rect outRect, View finalImage, ImageView imageView,
                    Runnable postRun) {
        this.finalImage = finalImage;
        this.finalImageView = imageView;
        this.postRun = postRun;
        this.setToRect(outRect);
        initOutAnim();
        new Thread(new Runnable() {
            public void run() {
                while (!inAnimFinished) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mIv.post(new Runnable() {
                    public void run() {
                        mIv.startAnimation(outAnim);
                    }
                });
            }
        }).start();
    }

    public boolean isAnimationStarted() {
        return isAnimationStarted;
    }

    @Override
    public void show() {
        ivLp.leftMargin = mFromRect.left;
        ivLp.topMargin = mFromRect.top;
        ivLp.width = mFromRect.right - mFromRect.left;
        ivLp.height = mFromRect.bottom - mFromRect.top;
        inAnimFinished = false;
        isAnimationStarted = false;
        this.postRun = null;
        super.show();
        if (toShowAnimation) {
            initInAnim();
            mIv.startAnimation(inAnim);
        } else {
            inAnimFinished = true;
            isAnimationStarted = true;
        }

        // Since this dialog prevents any other actions on the screen,
        // it's only allowed to be displayed no more than 5 seconds
        mFl.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, MaxShowingTime);
    }

    @Override
    public void dismiss() {
        emptyBitmap();
        if (!isShowing()) {
            return;
        }
        try {
            super.dismiss();
        } catch (Exception e) {

        }
    }
}
