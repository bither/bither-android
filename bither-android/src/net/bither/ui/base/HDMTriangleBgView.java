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

package net.bither.ui.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import net.bither.util.UIUtil;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/1/13.
 */
public class HDMTriangleBgView extends View {
    private static final int AnimationDuration = 800;

    private static final Paint paint = new Paint();
    private static final float LineWidth = UIUtil.dip2pix(2);
    private static final int LineColor = Color.argb(25, 0, 0, 0);

    static {
        paint.setColor(LineColor);
        paint.setStrokeWidth(LineWidth);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private ArrayList<Line> lines = new ArrayList<Line>();

    public HDMTriangleBgView(Context context) {
        super(context);
    }

    public HDMTriangleBgView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HDMTriangleBgView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        for (Line l : lines) {
            l.draw(canvas, paint);
        }
        super.onDraw(canvas);
    }


    public void addLine(View fromView, View toView) {
        float[] from = getViewPosition(fromView);
        float[] to = getViewPosition(toView);
        addLine(from[0], from[1], to[0], to[1]);
    }

    public void addLineAnimated(View fromView, View toView, Runnable completion) {
        float[] from = getViewPosition(fromView);
        float[] to = getViewPosition(toView);
        addLineAnimated(from[0], from[1], to[0], to[1], completion);
    }

    public void addLine(float startX, float startY, float endX, float endY) {
        Line l = new Line(startX, startY, endX, endY);
        if (!lines.contains(l)) {
            lines.add(l);
        }
        causeDraw();
    }

    public void addLineAnimated(float startX, float startY, float endX, float endY,
                                Runnable completion) {
        Line l = new Line(startX, startY, endX, endY, 0);
        if (!lines.contains(l)) {
            lines.add(l);
            l.beginAnim(completion);
        }
        causeDraw();
    }

    public void removeAllLines(){
        lines.clear();
        causeDraw();
    }

    private float[] getViewPosition(View v) {
        int[] myLocation = new int[2];
        int[] vLocation = new int[2];
        getLocationOnScreen(myLocation);
        v.getLocationOnScreen(vLocation);
        return new float[]{vLocation[0] + v.getWidth() / 2 - myLocation[0],
                vLocation[1] + v.getHeight() / 2 - myLocation[1]};
    }

    public void causeDraw() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private Runnable causeDrawRunnable = new Runnable() {
        @Override
        public void run() {
            causeDraw();
        }
    };

    public class Line {
        public float startX;
        public float startY;
        public float endX;
        public float endY;
        public float filledRate;

        private long beginAnimTime;
        private boolean animating;

        private Runnable animCompletion;

        public Line(float startX, float startY, float endX, float endY) {
            this(startX, startY, endX, endY, 1);
        }

        public Line(float startX, float startY, float endX, float endY, float filledRate) {
            this.startY = startY;
            this.startX = startX;
            this.endY = endY;
            this.endX = endX;
            this.filledRate = filledRate;
        }

        public void beginAnim(Runnable completion) {
            beginAnimTime = System.currentTimeMillis();
            animating = true;
            animCompletion = completion;
        }

        public float getDrawEndX() {
            float rate = rate();
            return (endX - startX) * rate + startX;
        }

        public float getDrawEndY() {
            float rate = rate();
            return (endY - startY) * rate + startY;
        }

        private float rate() {
            return Math.min(Math.max(filledRate, 0), 1);
        }

        public void draw(Canvas c, Paint paint) {
            if (animating) {
                filledRate = (float) (System.currentTimeMillis() - beginAnimTime) / (float)
                        AnimationDuration;
                if (filledRate >= 1) {
                    animCompleted();
                }
            }
            c.drawLine(startX, startY, getDrawEndX(), getDrawEndY(), paint);
            if (animating) {
                removeCallbacks(causeDrawRunnable);
                post(causeDrawRunnable);
            }
        }

        private void animCompleted() {
            animating = false;
            beginAnimTime = -1;
            filledRate = 1;
            if (animCompletion != null) {
                post(animCompletion);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Line) {
                Line l = (Line) o;
                return startX == l.startX && startY == l.startY && endX == l.endX && endY == l.endY;
            }
            return false;
        }
    }
}
