/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.xrandom.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.View;

import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 14-9-15.
 */
public class AudioVisualizerView extends View {
    private static final float LineWidth = UIUtil.dip2pix(1);
    private static final int LineColor = Color.WHITE;
    private static final int SubLineColor = Color.argb(100, 255, 255, 255);
    private static final int SubLineWidth = 1;
    private static final int SubLineCount = 5;
    private static final int HorizontalStraightLineLength = UIUtil.dip2pix(20);
    private static final float MinAmplitudeRate = 0.1f;
    private static final int WaveCount = 1;
    private static final long WaveDuration = 500;

    private static final int MinAmplitude = 15000;
    private static final int MaxAmplitude = 26000;

    private Paint paint;
    private Paint subLinePaint;

    private HandlerThread dataThread;
    private Handler dataHandler;

    private byte[] rawData;

    private AmplitudeData amplitudeData;

    private YCalculator yCalculator;

    private PathDrawer drawer;

    private boolean shouldDraw = true;


    public AudioVisualizerView(Context context) {
        super(context);
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }
        if (shouldDraw) {
            if (yCalculator == null) {
                yCalculator = new YCalculator();
            } else {
                yCalculator.reset();
            }

            drawer.draw();

            canvas.drawPath(drawer.main(), paint);

            for (Path p : drawer.subs()) {
                canvas.drawPath(p, subLinePaint);
            }
            invalidate();
        }
    }


    public void onPause() {
        shouldDraw = false;
        postInvalidate();
    }

    public void onResume() {
        shouldDraw = true;
    }

    private class PathDrawer {
        private int subLineCount;
        private Path main;
        private ArrayList<Path> subs;

        public PathDrawer() {
            this(0);
        }

        public PathDrawer(int subLineCount) {
            this.subLineCount = subLineCount;
        }

        public void draw() {
            main = new Path();
            subs = new ArrayList<Path>(subLineCount);
            yCalculator.amplitudeRate = 1;
            straightLineBefore(main);
            for (int i = 0;
                 i < subLineCount;
                 i++) {
                Path p = new Path();
                subs.add(p);
                yCalculator.amplitudeRate = rate(i);
                straightLineBefore(p);
            }

            for (int x = HorizontalStraightLineLength * 4;
                 x < yCalculator.width - HorizontalStraightLineLength * 4;
                 x++) {
                yCalculator.amplitudeRate = 1;
                main.lineTo(x, yCalculator.y(x));

                for (int i = 0;
                     i < subLineCount;
                     i++) {
                    yCalculator.amplitudeRate = rate(i);
                    subs.get(i).lineTo(x, yCalculator.y(x));
                }
            }

            yCalculator.amplitudeRate = 1;
            straightLineEnd(main);
            for (int i = 0;
                 i < subLineCount;
                 i++) {
                yCalculator.amplitudeRate = rate(i);
                straightLineEnd(subs.get(i));
            }
        }

        public Path main() {
            return main;
        }

        public List<Path> subs() {
            return subs;
        }

        private float rate(int index) {
            return (float) (index + 1) / (float) (subLineCount + 1);
        }

        private void straightLineBefore(Path path) {
            path.moveTo(0, yCalculator.height / 2);

            float controlY = yCalculator.y(HorizontalStraightLineLength * 3);

            path.quadTo(HorizontalStraightLineLength, yCalculator.height / 2,
                    HorizontalStraightLineLength * 2, (yCalculator.height / 2 + controlY) / 2);

            path.quadTo(HorizontalStraightLineLength * 3, controlY, HorizontalStraightLineLength
                    * 4, yCalculator.y(HorizontalStraightLineLength * 4));
        }

        private void straightLineEnd(Path path) {
            float controlY = yCalculator.y(yCalculator.width - HorizontalStraightLineLength * 3);

            path.quadTo(yCalculator.width - HorizontalStraightLineLength * 3, controlY,
                    yCalculator.width - HorizontalStraightLineLength * 2,
                    (yCalculator.height / 2 + controlY) / 2);

            path.quadTo(yCalculator.width - HorizontalStraightLineLength, yCalculator.height / 2,
                    yCalculator.width, yCalculator.height / 2);
        }
    }

    private class YCalculator {
        private long animBeginTime;
        private long currentTime;
        public int width;
        public int height;
        private int amplitude;
        private float xOffset;
        private float amplitudeRate;

        public YCalculator() {
            reset();
        }

        public void reset() {
            currentTime = System.currentTimeMillis();
            if (animBeginTime <= 0 || currentTime - animBeginTime > WaveDuration) {
                animBeginTime = currentTime;
            }

            width = getWidth();
            height = getHeight();

            if (amplitudeData != null) {
                amplitude = height * (amplitudeData.amplitude() - MinAmplitude) / (MaxAmplitude -
                        MinAmplitude);
                amplitude = Math.max(Math.min(height - (int) (2 * LineWidth), amplitude),
                        (int) (height * MinAmplitudeRate));
            } else {
                amplitude = (int) (height * MinAmplitudeRate);
            }

            xOffset = (float) (width - HorizontalStraightLineLength * 2) / (float) WaveCount /
                    (float) WaveDuration * (float) (currentTime - animBeginTime);

            amplitudeRate = 1;
        }

        public float y(float x) {
            return (float) (amplitude * amplitudeRate / 2.0f * Math.sin(2 * Math.PI * ((x -
                    xOffset) / (width - HorizontalStraightLineLength * 2)) *
                    WaveCount) + height / 2);
        }
    }

    public void onNewData(byte[] data) {
        rawData = data;
        if (dataHandler == null) {
            return;
        }
        dataHandler.removeCallbacks(analyzeData);
        dataHandler.post(analyzeData);
    }

    private Runnable analyzeData = new Runnable() {
        @Override
        public void run() {
            if (rawData != null && rawData.length > 0 && isVisible()) {
                amplitudeData = new AmplitudeData(rawData);
                rawData = null;
                postInvalidate();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (dataThread != null && dataThread.isAlive()) {
            dataThread.quit();
        }
        dataThread = new HandlerThread("AudioVisualizeDataThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        dataThread.start();
        dataHandler = new Handler(dataThread.getLooper());
        paint = new Paint();
        paint.setColor(LineColor);
        paint.setStrokeWidth(LineWidth);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        subLinePaint = new Paint(paint);
        subLinePaint.setStrokeWidth(SubLineWidth);
        subLinePaint.setColor(SubLineColor);

        drawer = new PathDrawer(SubLineCount);
    }

    @Override
    protected void onDetachedFromWindow() {
        dataHandler.removeCallbacksAndMessages(null);
        dataThread.quit();
        dataHandler = null;
        dataThread = null;
        super.onDetachedFromWindow();
    }

    private boolean isVisible() {
        return getGlobalVisibleRect(new Rect());
    }
}
