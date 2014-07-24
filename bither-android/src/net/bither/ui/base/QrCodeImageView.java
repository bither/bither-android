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

package net.bither.ui.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.bither.R;
import net.bither.util.Qr;
import net.bither.util.StringUtil;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class QrCodeImageView extends FrameLayout implements OnClickListener {
    public static interface QrCodeFullScreenListener {
        public void onShowFullScreenQr(String content, Bitmap placeHolder, View fromView);
    }

    private static ThreadPoolExecutor threadPool;
    private int fgColor;
    private int bgColor;
    private String content;
    private ImageView iv;
    private ProgressBar pb;
    private Bitmap bmp;
    private QrCodeFullScreenListener listener;
    private BmpRunnable bmpRunnable;
    private Future<?> future;
    private Handler mainHandler;
    private int qrCodeMargin = 0;

    public QrCodeImageView(Context context) {
        super(context);
        initView();
    }

    public QrCodeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public QrCodeImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setFullScreenListener(QrCodeFullScreenListener listener) {
        this.listener = listener;
    }

    private void initView() {
        fgColor = Color.BLACK;
        bgColor = Color.TRANSPARENT;
        mainHandler = new Handler(Looper.getMainLooper());
        removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.layout_qr_code_imageview, this);
        iv = (ImageView) findViewById(R.id.iv_qr);
        pb = (ProgressBar) findViewById(R.id.pb_qr);
        setOnClickListener(this);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        setContent(content, Color.BLACK, Color.TRANSPARENT);
    }

    public void setContent(String content, int fgColor, int bgColor) {
        setContent(content, fgColor, bgColor, 0);
    }

    public void setContent(String content, int fgColor, int bgColor, int qrCodeMargin) {
        if (!StringUtil.compareString(this.content, content) || bgColor != this.bgColor ||
                fgColor != this.fgColor || this.qrCodeMargin != qrCodeMargin) {
            this.bgColor = bgColor;
            this.fgColor = fgColor;
            this.qrCodeMargin = qrCodeMargin;
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            mainHandler.removeCallbacks(showRunnable);
            bmp = null;
            if (!StringUtil.compareString(this.content, content)) {
                iv.setImageBitmap(null);
            }
            this.content = content;
            if (getWidth() == 0 || getHeight() == 0) {
                mainHandler.postDelayed(showRunnable, 50);
            } else {
                showRunnable.run();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (bmp == null || listener == null) {
            return;
        }
        listener.onShowFullScreenQr(content, bmp, this);
    }

    private ThreadPoolExecutor getThreadPool() {
        if (threadPool == null || threadPool.isShutdown()) {
            threadPool = new ThreadPoolExecutor(0, 2, 5, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }
        return threadPool;
    }

    private class BmpRunnable implements Runnable {
        private int size;

        public BmpRunnable(int size) {
            this.size = size;
        }

        @Override
        public void run() {
            if (Thread.interrupted()) {
                return;
            }
            if (StringUtil.isEmpty(content)) {
                return;
            }
            bmp = Qr.bitmap(QrCodeImageView.this.content, size, fgColor, bgColor, qrCodeMargin);
            if (Thread.interrupted()) {
                return;
            }
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    iv.setImageBitmap(bmp);
                    pb.setVisibility(View.GONE);
                }
            });
            future = null;
        }
    }

    private Runnable showRunnable = new Runnable() {

        @Override
        public void run() {
            final int size = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                    getHeight() - getPaddingTop() - getPaddingBottom());
            if (size == 0) {
                mainHandler.removeCallbacks(showRunnable);
                mainHandler.postDelayed(showRunnable, 50);
                return;
            }
            iv.getLayoutParams().width = size;
            iv.getLayoutParams().height = size;
            pb.setVisibility(View.VISIBLE);
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            bmpRunnable = new BmpRunnable(size);
            future = getThreadPool().submit(bmpRunnable);
        }
    };


}
