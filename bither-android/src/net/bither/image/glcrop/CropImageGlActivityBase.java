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
package net.bither.image.glcrop;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;


import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.ImageFileUtil;
import net.bither.util.LogUtil;
import net.bither.util.StringUtil;
import net.bither.runnable.BaseRunnable;
import net.bither.util.ImageManageUtil;
import net.bither.animation.FlipAndZoomAnimation;
import net.bither.util.FileUtil;


/**
 * The activity can crop specific region of interest from an image.
 */
public abstract class CropImageGlActivityBase extends Activity {

    public static final int CROP_MSG = 10;
    public static final int CROP_MSG_INTERNAL = 100;

    private boolean mCircleCrop = false;
    private final Handler mHandler = new Handler();

    public boolean mWaitingToPick; // Whether we are wait the user to pick a
    // face.
    public boolean mSaving; // Whether the "save" button is already clicked.

    //  private FilterSelectorView fv;
    private CropImageView mImageView;
    // private FilterSurfaceView cv;
    private FrameLayout flCameraIrisFrame;
    private FrameLayout flFilterImage;
    private FrameLayout flImageContainer;
    private FrameLayout flCamContainer;
    private FrameLayout flFilterToggle;
    private ToggleButton tbtnFilters;
    private FrameLayout flFrameToggle;
    private ToggleButton tbtnFrame;
    private ImageButton ibtnTiltShift;
    //  private TiltShiftModeView tiltShiftModeView;

    //private List<IFilter> mFilters;
    private Bitmap mBitmap;
    private Bitmap orBitmap;
    private Bitmap filterBitmap;

    private ImageButton btnCrop;
    int orientation = 0;
    private ImageButton IV90R;

    private static final float zTransition = 400;
    // private MediaItem mItem;
    // private final BitmapManager.ThreadSet mDecodingThreads = new
    // BitmapManager.ThreadSet();
    public HighlightView mCrop;

    //private IFilter selectedFilter;

    private static final int CropSide = 0;
    private static final int FilterSide = 1;
    private int side = CropSide;
    private String fromFileName;
    // private String saveFileName;
    private long timeMillis;

    private Dialog pdSaving;

    private boolean isPaused = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        setContentView(R.layout.activity_gl_crop_image);
        Intent intent = getIntent();
        if (StringUtil.compareString(intent.getAction(), Intent.ACTION_SEND)) {
            if (intent.getExtras().containsKey("android.intent.extra.STREAM")) {
                if (BitherApplication.initialActivity != null) {
                    BitherApplication.initialActivity.finish();
                }
//                FetcherHolder.getLargeImageFetcher().getImageCache()
//                        .clearMemoryCache();
//                FetcherHolder.getSmallImageFetcher().getImageCache()
//                        .clearMemoryCache();
                Uri formUri = (Uri) intent.getExtras().get(
                        "android.intent.extra.STREAM");
                File fromFile = FileUtil.convertUriToFile(
                        CropImageGlActivityBase.this, formUri);
                if (fromFile != null) {
                    fromFileName = fromFile.getAbsolutePath();
                }
            }
        }
        if (StringUtil.isEmpty(fromFileName)) {
            Uri formUri = intent.getData();
            if (formUri != null) {
                File fromFile = FileUtil.convertUriToFile(
                        CropImageGlActivityBase.this, formUri);
                if (fromFile != null) {
                    fromFileName = fromFile.getAbsolutePath();
                }
            }
        }
        if (StringUtil.isEmpty(fromFileName)) {
            finish();
            return;
        }
        orBitmap = getOrBitmap();
        if (orBitmap == null) {
            finish();
            return;
        }

        mImageView = (CropImageView) findViewById(R.id.image);
        //  cv = (FilterSurfaceView) findViewById(R.id.filter_image);
        flCameraIrisFrame = (FrameLayout) findViewById(R.id.fl_camera_iris_frame);
        LayoutParams lp = (LayoutParams) flCameraIrisFrame
                .getLayoutParams();
        lp.height = ImageManageUtil.getScreenWidth() - lp.rightMargin
                - lp.leftMargin;
        flFilterImage = (FrameLayout) findViewById(R.id.fl_filter_image);
        flImageContainer = (FrameLayout) findViewById(R.id.fl_image_container);
        flCamContainer = (FrameLayout) findViewById(R.id.fl_cam_container);
        flFilterToggle = (FrameLayout) findViewById(R.id.fl_filter_toggle);
        flFrameToggle = (FrameLayout) findViewById(R.id.fl_frame_toggle);
        tbtnFrame = (ToggleButton) findViewById(R.id.tbtn_frame);
        ibtnTiltShift = (ImageButton) findViewById(R.id.ibtn_tilt_shift);
        //    tiltShiftModeView = (TiltShiftModeView) findViewById(R.id.tiltShift);
        flFilterToggle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tbtnFilters.setChecked(!tbtnFilters.isChecked());
            }
        });
        tbtnFilters = (ToggleButton) findViewById(R.id.tbtn_filter);
        tbtnFilters.setOnCheckedChangeListener(filtersCheck);
        //fv = (FilterSelectorView) findViewById(R.id.sv_filters);
        findViewById(R.id.discard).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                        overridePendingTransition(0, R.anim.slide_out_bottom);
                    }
                }
        );
        findViewById(R.id.ibtn_discard).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                        overridePendingTransition(0, R.anim.slide_out_bottom);
                    }
                }
        );

        findViewById(R.id.ibtn_save).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        onSaveClicked();
                    }
                }
        );

        IV90R = (ImageButton) findViewById(R.id.image_cw_90r);
        btnCrop = (ImageButton) findViewById(R.id.btn_crop);
        IV90R.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (side == CropSide) {
                    orientation += 90;
                    rotateImage(90);
                    startRedrawImageView();
                }
            }
        });
        btnCrop.setOnClickListener(cropOnClick);

        flFrameToggle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tbtnFrame.setChecked(!tbtnFrame.isChecked());
            }
        });
//        tbtnFrame.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView,
//                                         boolean isChecked) {
//                cv.setDrawBorder(isChecked);
//            }
//        });
        ibtnTiltShift.setOnClickListener(tiltShiftClick);
        // tiltShiftModeView.setModeSelectedListener(tiltShiftModeSelected);
//        mFilters = GlFilterUtil.getFilters();
//        cv.setFilters(mFilters);
//        initFilterSelector();
        orientation = FileUtil.getOrientationOfFile(fromFileName);
        rotateImage(orientation);
        startRedrawImageView();
        tbtnFilters.postDelayed(new Runnable() {
            public void run() {
                tbtnFilters.setChecked(true);
            }
        }, 100);

        pdSaving = getProgressDialog(getString(R.string.saving));
        pdSaving.setCancelable(true);
//        FetcherHolder.getLargeImageFetcher();
//        FetcherHolder.getSmallImageFetcher();
    }

//    private ModeSelectedListener tiltShiftModeSelected = new ModeSelectedListener() {
//        public void onModeSelected(int mode) {
//            if (mode != BlurManager.getBlurType()) {
//                BlurManager.setBlurType(mode);
//                if (mode == BlurManager.BLUR_TYPE_NONE) {
//                    ibtnTiltShift
//                            .setImageResource(R.drawable.action_bar_glyph_tiltshift_selector);
//                    cv.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//                    cv.requestRender();
//                } else {
//                    if (mode == BlurManager.BLUR_TYPE_CIRCLE) {
//                        ibtnTiltShift
//                                .setImageResource(R.drawable.action_bar_glyph_tiltshift_circle_selector);
//                    } else if (mode == BlurManager.BLUR_TYPE_LINEAR) {
//                        ibtnTiltShift
//                                .setImageResource(R.drawable.action_bar_glyph_tiltshift_bar_selector);
//                    }
//                    BlurManager.showOnce();
//                }
//            }
//        }
//    };

    private OnClickListener tiltShiftClick = new OnClickListener() {
        public void onClick(View v) {
//            if (tiltShiftModeView.getVisibility() != View.VISIBLE) {
//                ((RelativeLayout.LayoutParams) tiltShiftModeView
//                        .getLayoutParams()).leftMargin = v.getLeft();
//                tiltShiftModeView.setVisibility(View.VISIBLE);
//            } else {
//                tiltShiftModeView.setVisibility(View.GONE);
//            }
        }
    };

    private OnCheckedChangeListener filtersCheck = new OnCheckedChangeListener() {

        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
//            flFilterToggle.setEnabled(false);
//            if (isChecked) {
//                fv.setVisibility(View.VISIBLE);
//                fv.clearAnimation();
//                Animation show = AnimationUtils.loadAnimation(
//                        CropImageGlActivityBase.this,
//                        R.anim.camera_filters_show);
//                show.setAnimationListener(new AnimationListener() {
//                    public void onAnimationStart(Animation animation) {
//
//                    }
//
//                    public void onAnimationRepeat(Animation animation) {
//
//                    }
//
//                    public void onAnimationEnd(Animation animation) {
//                        int restArea = flCamContainer.getHeight()
//                                - cv.getHeight();
//                        ((LayoutParams) flImageContainer
//                                .getLayoutParams()).bottomMargin = Math.min(
//                                restArea, fv.getHeight());
//                        flImageContainer.requestLayout();
//                        flFilterToggle.setEnabled(true);
//                    }
//                });
//                fv.startAnimation(show);
//            } else {
//                Animation hide = AnimationUtils.loadAnimation(
//                        CropImageGlActivityBase.this,
//                        R.anim.camera_filters_hide);
//                hide.setAnimationListener(new AnimationListener() {
//                    public void onAnimationStart(Animation animation) {
//                    }
//
//                    public void onAnimationRepeat(Animation animation) {
//                    }
//
//                    public void onAnimationEnd(Animation animation) {
//                        fv.setVisibility(View.GONE);
//                        flFilterToggle.setEnabled(true);
//                    }
//                });
//                fv.clearAnimation();
//                fv.startAnimation(hide);
//                ((LayoutParams) flImageContainer.getLayoutParams()).bottomMargin = 0;
//            }
        }
    };

    private Bitmap getCropedBitmap() {
        Rect r = mCrop.getCropRect();
        // OpenGl texture must be pow of 2
        int size = Math.min(r.width(), r.height());
        if (size % 2 != 0) {
            size--;
        }
        r.bottom = r.top + size;
        r.right = r.left + size;
        int width = r.width();
        int height = r.height();
        width = Math.min(width,
                ImageManageUtil.IMAGE_SIZE);
        height = Math.min(height,
                ImageManageUtil.IMAGE_SIZE);
        Bitmap croppedImage = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        {
            Canvas canvas = new Canvas(croppedImage);
            Rect dstRect = new Rect(0, 0, width, height);
            if (orBitmap == null) {
                rotateImage(orientation);
            }
            canvas.drawBitmap(orBitmap, r, dstRect, null);
        }

        return croppedImage;
    }

    private void initFilterSelector() {
//        fv.setFilters(mFilters);
//        fv.setOnFilterSelectedListener(new OnFilterSelectedListener() {
//            public void OnFilterSelected(IFilter filter) {
//                selectedFilter = filter;
//                if (selectedFilter != null) {
//                    if (side != FilterSide) {
//                        if (!(selectedFilter instanceof NormalFilter)) {
//                            turnToFilter();
//                        }
//                    } else {
//                        cv.setFilter(selectedFilter);
//                    }
//                }
//            }
//        });
//        fv.setSelectedFilter(mFilters.get(0));
    }

    private OnClickListener cropOnClick = new OnClickListener() {

        public void onClick(View v) {
            try {
                orBitmap = null;
                turnToCrop();
                rotateImage(orientation);
                mImageView.setImageBitmapResetBase(orBitmap, false);

            } catch (Exception e) {
                //   LogUtil.recordException(this.toString(), e);
            }
        }
    };

    private void rotateImage(int rotation) {
        this.orBitmap = getOrBitmap();
        if (this.orBitmap != null) {
            int width = this.orBitmap.getWidth();
            int hegith = this.orBitmap.getHeight();

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            this.orBitmap = Bitmap.createBitmap(this.orBitmap, 0, 0, width,
                    hegith, matrix, false);
        }
    }

    private void startRedrawImageView() {
        if (isFinishing()) {
            return;
        }

        mImageView.setImageBitmapResetBase(orBitmap, true);
        BaseRunnable runnable = new BaseRunnable() {
            @Override
            public void run() {
                final CountDownLatch latch = new CountDownLatch(1);
                mHandler.post(new Runnable() {
                    public void run() {
                        if (mImageView.getScale() == 1.0f) {
                            mImageView.center(true, true);
                        }
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mRunDrawImageView.run();
            }
        };
        runnable.setHandler(mHandler);
        new Thread(runnable).start();
    }

    @Override
    protected void onResume() {
        isPaused = false;
//        if (cv != null) {
//            if (side == FilterSide) {
//                cv.onResume();
//            }
//        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        isPaused = true;
//        if (cv != null) {
//            cv.onPause();
//        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        completeRecycle();
    }

    private void completeRecycle() {
        if (mImageView != null) {
            mImageView.clear();
        }
        if (mBitmap != null && !mBitmap.isRecycled()
                && (side == CropSide)) {
            mBitmap = null;
        }
        if (orBitmap != null && !orBitmap.isRecycled()) {
            orBitmap = null;
        }
        if (filterBitmap != null && !filterBitmap.isRecycled()
                && (side == CropSide)) {
            filterBitmap = null;
        }
        //   GLUtil.releaseDirectIntBuffer();
    }

    private void onSaveClicked() {

        try {
            if (mSaving)
                return;

            if (side == CropSide && mCrop == null) {
                return;
            }

            mSaving = true;

            timeMillis = System.currentTimeMillis();
            Bitmap croppedImage;
            final Rect imagePlace = new Rect();
            final String photoName;
//            switch (getPiImageType()) {
//                case GETCAI:
//                    photoName = FileUtil.getGetcaiPhotoName(AppSharedPreference
//                            .getInstance().getMyUser().getUserId(), timeMillis);
//                    break;
//                case AVATAR:
//                    ImageFileUtil.getAvatarFile()
//                    photoName = FileUtil.getSelfAvatarPhotoName(timeMillis);
//                    break;
//                default:
//                    photoName = FileUtil.getPiameraPhotoName(AppSharedPreference
//                            .getInstance().getMyUser().getUserId(), timeMillis);
//                    break;
//            }
            if (side == CropSide) {
                croppedImage = getCropedBitmap();
                Rect crop = mCrop.getCropRect();
                int[] location = new int[2];
                flImageContainer.getLocationOnScreen(location);
                Matrix imageMatrix = mImageView.getImageMatrix();
                Rect bitmapRect = mImageView.getDrawable().copyBounds();
                float[] values = new float[9];
                imageMatrix.getValues(values);
                float visualWidth = bitmapRect.width() * values[0];
                float visualHeight = bitmapRect.height() * values[0];
                Rect drawRect = new Rect((int) values[2], (int) values[5],
                        (int) (values[2] + visualWidth),
                        (int) (values[5] + visualHeight));
                imagePlace.left = (int) (location[0] + drawRect.left + crop.left
                        * values[0]);
                imagePlace.top = (int) (location[1] + drawRect.top + crop.top
                        * values[0]);
                imagePlace.right = imagePlace.left
                        + (int) (crop.width() * values[0]);
                imagePlace.bottom = imagePlace.top
                        + (int) (crop.height() * values[0]);
                SaveRunnable save = new SaveRunnable(croppedImage, timeMillis
                );
                save.setHandler(new Handler() {
                    @Override
                    public void dispatchMessage(Message msg) {

//                        switch (msg.what) {
//                            case GLUtil.CaptureBegin:
//                                pdSaving.show();
//                                break;
//                            case GLUtil.CaptureSuccess:
//                                mSaving = false;
//                                pdSaving.dismiss();
//                                BitherApplication.getDialogCropPhotoTransit()
//                                        .setFromRect(imagePlace);
//                                BitherApplication.getDialogCropPhotoTransit()
//                                        .setToShowAnimation(toShowSaveAnimation());
//                                BitherApplication
//                                        .getDialogCropPhotoTransit()
//                                        .setBitmap(
//                                                FetcherHolder
//                                                        .getLargeImageFetcher()
//                                                        .getImageCache()
//                                                        .getBitmapFromMemCache(
//                                                                getPiImageType()
//                                                                        .getLargeImageUrl(
//                                                                                photoName)
//                                                        )
//                                        );
//                                if (!isPaused) {
//                                    BitherApplication.getDialogCropPhotoTransit()
//                                            .show();
//                                }
//                                handleSaveSuccess(timeMillis, 0);
//                                break;
//                            case GLUtil.CaptureFailed:
//                                mSaving = false;
//
//                                pdSaving.dismiss();
//                                break;
//                        }
                    }
                });
                new Thread(save).start();
            } else {
                //final int filterId = cv.getFilter().getFilterId();
                int[] location = new int[2];
                //   cv.getLocationOnScreen(location);
                imagePlace.top = location[1];
                int imageSize = ImageManageUtil.IMAGE_SIZE;
                imagePlace.bottom = imagePlace.top + imageSize;
                imagePlace.left = location[0];
                imagePlace.right = location[0] + imageSize;
                Handler h = new Handler() {
                    @Override
                    public void dispatchMessage(Message msg) {
                        switch (msg.what) {
//                            case GLUtil.CaptureBegin:
//                                pdSaving.show();
//                                break;
//                            case GLUtil.CaptureSuccess:
//                                mSaving = false;
//                                pdSaving.dismiss();
//                                PiApplication.getDialogCropPhotoTransit()
//                                        .setFromRect(imagePlace);
//                                PiApplication.getDialogCropPhotoTransit()
//                                        .setToShowAnimation(toShowSaveAnimation());
//                                PiApplication
//                                        .getDialogCropPhotoTransit()
//                                        .setBitmap(
//                                                FetcherHolder
//                                                        .getLargeImageFetcher()
//                                                        .getImageCache()
//                                                        .getBitmapFromMemCache(
//                                                                getPiImageType()
//                                                                        .getLargeImageUrl(
//                                                                                photoName)
//                                                        )
//                                        );
//                                if (!isPaused) {
//                                    BitherApplication.getDialogCropPhotoTransit()
//                                            .show();
//                                }
//                                handleSaveSuccess(timeMillis, filterId);
//                                break;
//                            case GLUtil.CaptureFailed:
//                                mSaving = false;
//
//                                pdSaving.dismiss();
//                                orBitmap = getOrBitmap();
//                                turnToCrop();
//                                startRedrawImageView();
//                                break;
//                            default:
//                                break;
                        }
                        super.dispatchMessage(msg);
                    }
                };
                orBitmap = null;
                mBitmap = null;
                mImageView.setImageBitmapResetBase(null, false);
                filterBitmap = null;
                //    cv.save(h, timeMillis, getPiImageType());
            }
        } catch (Exception e) {
            //  LogUtil.recordException(this.toString(), e);
        }

    }

    private static class SaveRunnable extends BaseRunnable {

        private Bitmap b;
        private long timeMillis;
//        private PiImageType imageType;

        public SaveRunnable(Bitmap bmp, long timeStamp) {
            this.b = bmp;
            this.timeMillis = timeStamp;
//            this.imageType = imageType;
        }

        @Override
        public void run() {
            try {
                //TODO save bitmap
//                obtainMessage(GLUtil.CaptureBegin);
//                GLUtil.saveImage(b, timeMillis, 0, false, imageType);
//                obtainMessage(GLUtil.CaptureSuccess);
            } catch (Exception e) {
                e.printStackTrace();
                //  obtainMessage(GLUtil.CaptureFailed);
            }
        }
    }

    private void turnToFilter() {
        if (side == CropSide && mCrop == null) {
            return;
        }
        IV90R.setClickable(false);
        mBitmap = null;
        mImageView.setImageBitmapResetBase(null, true);
        mBitmap = getCropedBitmap();
        orBitmap = null;
//        cv.setSource(mBitmap);
//        cv.setFilter(selectedFilter);
//        cv.onResume();
        applyRotation(flImageContainer, 0, 90, 0, zTransition,
                toFilterPreRotate);
    }

    private void turnToCrop() {

        applyRotation(flImageContainer, 0, -90, 0, zTransition, toCropPreRotate);
        btnCrop.setClickable(false);
        ibtnTiltShift.setVisibility(View.GONE);
    }

    private Bitmap getOrBitmap() {
        try {
            if (orBitmap == null || orBitmap.isRecycled()) {
                if (!StringUtil.isEmpty(fromFileName)) {
                    orBitmap = ImageManageUtil
                            .getBitmapNearestSize(fromFileName, ImageManageUtil.IMAGE_SIZE);
                    if (orBitmap == null) {
                        finish();
                        return null;
                    }
                    orBitmap = ImageManageUtil
                            .getMatrixBitmap(
                                    orBitmap,
                                    ImageManageUtil.IMAGE_SIZE,
                                    ImageManageUtil.IMAGE_SIZE,
                                    true
                            );
                }
            }
            return orBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return null;
        }
    }

    private void applyRotation(View view, float startDegree, float endDegree,
                               float fromZ, float toZ, AnimationListener listener) {
        final float centerX = view.getWidth() / 2.0f;
        final float centerY = view.getHeight() / 2.0f;
        final FlipAndZoomAnimation rotation = new FlipAndZoomAnimation(
                startDegree, endDegree, fromZ, toZ, centerX, centerY);
        rotation.setDuration(300);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        if (listener != null) {
            rotation.setAnimationListener(listener);
        }
        view.startAnimation(rotation);
    }

    private AnimationListener toFilterPreRotate = new AnimationListener() {

        public void onAnimationStart(Animation animation) {
            mImageView.setTouchable(false);
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            flFilterImage.bringToFront();
            //  cv.requestRender();
            applyRotation(flImageContainer, -90, 0, zTransition, 0,
                    new PostRotate(FilterSide));
        }
    };

    private AnimationListener toCropPreRotate = new AnimationListener() {

        public void onAnimationStart(Animation animation) {
            mImageView.setTouchable(false);
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {

            mImageView.bringToFront();
            //  cv.onPause();
            applyRotation(flImageContainer, 90, 0, zTransition, 0,
                    new PostRotate(CropSide));
        }
    };

    private class PostRotate implements AnimationListener {

        private int side;

        public PostRotate(int side) {
            this.side = side;
        }

        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            CropImageGlActivityBase.this.side = side;
            if (side == CropSide) {
                mImageView.setTouchable(true);
                IV90R.setClickable(true);
                IV90R.setVisibility(View.VISIBLE);
                btnCrop.setVisibility(View.GONE);
                //fv.setSelectedFilter(mFilters.get(0));
            } else if (side == FilterSide) {
                btnCrop.setClickable(true);
                mImageView.setTouchable(false);
                IV90R.setVisibility(View.GONE);
                btnCrop.setVisibility(View.VISIBLE);
                ibtnTiltShift.setVisibility(View.VISIBLE);
                mImageView.setImageBitmapResetBase(null, true);
            }
        }
    }

    ;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED);
            finish();
            overridePendingTransition(0, R.anim.slide_out_bottom);
        }
        return super.onKeyDown(keyCode, event);
    }

    ;

    Runnable mRunDrawImageView = new Runnable() {
        float mScale = 1F;
        Matrix mImageMatrix;

        // Create a default HightlightView if we found no face in the picture.
        private void makeDefault() {
            HighlightView hv = new HighlightView(mImageView);

            int width = orBitmap.getWidth();
            int height = orBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            // CR: sentences!
            // make the default size about 4/5 of the width or height
            int cropWidth = Math.min(width, height);
            int cropHeight = cropWidth;

            int x = (width - cropWidth) / 2;
            int y = (height - cropHeight) / 2;

            RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
            hv.setup(mImageMatrix, imageRect, cropRect, mCircleCrop, false);
            mImageView.add(hv);
        }

        public void run() {
            mImageMatrix = mImageView.getImageMatrix();

            mScale = 1.0F / mScale;

            mHandler.post(new Runnable() {
                public void run() {

                    makeDefault();

                    mImageView.invalidate();
                    if (mImageView.mHighlightViews.size() == 1) {
                        mCrop = mImageView.mHighlightViews.get(0);
                        mCrop.setFocus(true);
                    }
                }
            });
        }
    };

    protected Boolean toShowSaveAnimation() {
        return true;
    }

    protected abstract void handleSaveSuccess(long timeStamp, int filterId);

    protected abstract Dialog getProgressDialog(String msg);
}
