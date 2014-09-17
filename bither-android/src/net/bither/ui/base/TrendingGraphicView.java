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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import net.bither.BitherSetting.MarketType;
import net.bither.model.TrendingGraphicData;
import net.bither.util.TrendingGraphicUtil;
import net.bither.util.TrendingGraphicUtil.TrendingGraphicListener;
import net.bither.util.UIUtil;

import java.util.Arrays;

public class TrendingGraphicView extends View {
    private static final int TransformDuration = 250;
    private static final float LineWidth = UIUtil.dip2pix(2);
    private static final float HorizontalPadding = UIUtil.dip2pix(2);
    private static final float VerticalPadding = UIUtil.dip2pix(2);
    private static final int LineColor = Color.WHITE;
    private static final int PressedLineColor = Color.argb(190, 180, 180, 180);
    private static final Paint paint = new Paint();

    static {
        paint.setColor(LineColor);
        paint.setStrokeWidth(LineWidth);
        paint.setAntiAlias(true);
        paint.setStyle(Style.STROKE);
        paint.setStrokeJoin(Join.ROUND);
        paint.setStrokeCap(Cap.ROUND);
    }

    private static final Rect GlobalRect = new Rect();

    private View parent;

    private RateAnimation rateAnimation;

    private MarketType market;

    private Runnable retryRunnable = new Runnable() {

        @Override
        public void run() {
            if (getGlobalVisibleRect(GlobalRect)) {
                setMarketType(market);
            }
        }
    };

    private boolean isFirstDraw = true;

    public TrendingGraphicView(Context context) {
        super(context);
        initView();
    }

    public TrendingGraphicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TrendingGraphicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public boolean isFirstDraw() {
        return isFirstDraw;
    }

    private void initView() {
        setData(null);
    }

    public void setMarketType(MarketType market) {
        removeCallbacks(retryRunnable);
        TrendingGraphicData data = TrendingGraphicUtil.getTrendingGraphicData(
                market, new TrendingGraphicListener() {

                    @Override
                    public void success(TrendingGraphicData trendingGraphicData) {
                        setData(trendingGraphicData);
                    }

                    @Override
                    public void error() {
                        postDelayed(retryRunnable, 30000);
                    }
                }
        );
        if (market != this.market && data == null) {
            setData(null);
        }
        if (data != null) {
            setData(data);
        }
        this.market = market;
    }

    public void setData(TrendingGraphicData data) {
        if (data == null) {
            setData(TrendingGraphicData.getEmptyData());
            return;
        }
        if (rateAnimation == null) {
            rateAnimation = new RateAnimation(TrendingGraphicData
                    .getEmptyData().getRates(), data.getRates());
        } else {
            RateAnimation anim = new RateAnimation(
                    rateAnimation.getCurrenRates(), data.getRates());
            rateAnimation = anim;
        }
        causeDraw();
    }

    private void drawRates(double[] rates, Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int length = rates.length;
        int step = getPointStep(length, width);
        Path path = new Path();
        float preX = getPointX(0, width, step, length);
        float preY = getPointY(rates[0], height);
        path.moveTo(preX, preY);
        for (int i = 0 + step;
             i < length;
             i += step) {
            float x = getPointX(i, width, step, length);
            float y = getPointY(rates[i], height);
            float midX = (preX + x) / 2;
            float midY = (preY + y) / 2;
            if (i == step) {
                path.lineTo(midX, midY);
            } else {
                path.quadTo(preX, preY, midX, midY);
            }
            preX = x;
            preY = y;
        }
        path.lineTo(preX, preY);
        if (isPressed()) {
            paint.setColor(PressedLineColor);
            canvas.drawPath(path, paint);
            causeDraw();
        } else {
            paint.setColor(LineColor);
            canvas.drawPath(path, paint);
        }
    }

    public void causeDraw() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private float getPointY(double rate, int height) {
        return (float) ((height - VerticalPadding * 2) * (1.0 - rate) + VerticalPadding);
    }

    private int getPointStep(int pointCount, int width) {
        int step = 1;
        if (pointCount > (width - HorizontalPadding * 2)) {
            step = (int) (pointCount / (width - HorizontalPadding * 2));
        }
        return step;
    }

    private float getPointX(int index, int width, int step, int dataLength) {
        int pointCount = dataLength / step;
        int pointIndex = index / step;
        float pointRate = (float) pointIndex / (float) (pointCount - 1);
        return (width - HorizontalPadding * 2) * pointRate + HorizontalPadding;
    }

    @Override
    public boolean isPressed() {
        if (super.isPressed()) {
            return true;
        }
        // workaround for haier phone
        if (getGlobalVisibleRect(GlobalRect)) {
            if (parent == null) {
                parent = (View) getParent();
            }
            if (parent != null) {
                return parent.isPressed();
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!getGlobalVisibleRect(GlobalRect) && !isFirstDraw) {
            setMarketType(market);
        } else {
            canvas.drawARGB(0, 0, 0, 0);
            if (rateAnimation != null) {
                double[] rates = rateAnimation.getCurrenRates();
                drawRates(rates, canvas);
                super.onDraw(canvas);
                rateAnimation.afterDraw();
            } else {
                super.onDraw(canvas);
            }
        }
        if (isFirstDraw) {
            isFirstDraw = false;
        }
    }

    private class RateAnimation {
        private long startTime;

        private double[] startRates;
        private double[] endRates;

        public RateAnimation(double[] startRates, double[] endRates) {
            this.startRates = startRates;
            this.endRates = endRates;
        }

        public double[] getCurrenRates() {
            if (Arrays.equals(startRates, endRates)) {
                return endRates;
            }
            if (startTime <= 0) {
                startTime = System.currentTimeMillis();
            }
            long animTime = System.currentTimeMillis() - startTime;
            double progress = (double) animTime / (double) TransformDuration;
            progress = Math.max(0, Math.min(1, progress));
            double[] rates = new double[startRates.length];
            for (int i = 0;
                 i < rates.length;
                 i++) {
                rates[i] = progress * (endRates[i] - startRates[i])
                        + startRates[i];
            }
            return rates;
        }

        public void afterDraw() {
            if (isRunning()) {
                causeDraw();
            }
        }

        public boolean isRunning() {
            if (startTime > 0
                    && startTime + TransformDuration > System
                    .currentTimeMillis()) {
                return true;
            }
            return false;
        }
    }
}
