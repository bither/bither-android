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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;

import java.math.BigInteger;
import java.util.ArrayList;

public class PieChartView extends View {
    public static interface RotateListener {
        public void onRotationChanged(float rotation);
    }

    public static final int[] Colors = new int[]{Color.rgb(194, 253, 70), Color.rgb(100, 114, 253),
                                                Color.rgb(38, 230, 91), Color.rgb(254, 35, 93),
                                                Color.rgb(36, 182, 212), Color.rgb(237, 193, 155),
                                                Color.rgb(117, 140, 129), Color.rgb(200, 47, 217),
                                                Color.rgb(239, 204, 41), Color.rgb(253, 38, 38),
                                                Color.rgb(253, 160, 38), Color.rgb(144, 183, 177)};
    public static final float DefaultStartAngle = 90;
    private static final int TransformDuration = 400;
    private static final float MaxTotalAngle = 360;
    private static final float MinRate = 0.03f;
    private static final Paint paint = new Paint();

    static {
        paint.setAntiAlias(true);
    }

    private BigInteger[] amounts;
    private BigInteger total;
    private float startAngle = DefaultStartAngle;

    private float totalAngle = 0;

    private RotateListener rotateListener;

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAmounts(BigInteger... amounts) {
        this.amounts = amounts;
        total = BigInteger.ZERO;
        if (amounts != null) {
            for (BigInteger i : amounts) {
                total = total.add(i);
            }
        }
        causeDraw();
        if (getTotalAngle() < MaxTotalAngle) {
            ObjectAnimator.ofFloat(this, "totalAngle", MaxTotalAngle).setDuration
                    (TransformDuration).start();
        }
    }

    public void setStartAngle(float angle) {
        this.startAngle = angle;
        notifyRotateListener();
        causeDraw();
    }

    public void setStartAngleRound360(float angle) {
        if (angle < 0) {
            angle = angle + (float) Math.ceil(angle / -360.0) * 360.0f;
        }
        if (angle >= 360) {
            angle = angle % 360;
        }
        setStartAngle(angle);
    }

    public float getStartAngle() {
        return startAngle;
    }

    public void setTotalAngle(float total) {
        if (total <= MaxTotalAngle) {
            totalAngle = total;
            inertia.reset();
            setStartAngle(DefaultStartAngle);
            causeDraw();
        }
    }

    public float getTotalAngle() {
        return totalAngle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        RectF rect = new RectF(0, 0, getWidth(), getHeight());
        float angle = this.startAngle;
        ArrayList<Integer> minIndexes = new ArrayList<Integer>();
        if (amounts != null && amounts.length > 0 && total.signum() > 0) {
            float[] rates = new float[amounts.length];
            for (int i = 0;
                 i < amounts.length;
                 i++) {
                rates[i] = amounts[i].floatValue() / total.floatValue();
                if (rates[i] > 0 && rates[i] < MinRate) {
                    minIndexes.add(i);
                    rates[i] = MinRate;
                }
            }

            if (minIndexes.size() > 0) {
                BigInteger total = BigInteger.ZERO;
                for (int i = 0;
                     i < amounts.length;
                     i++) {
                    if (!minIndexes.contains(Integer.valueOf(i))) {
                        total = total.add(amounts[i]);
                    }
                }
                float restRate = 1.0f - minIndexes.size() * MinRate;
                for (int i = 0;
                     i < amounts.length;
                     i++) {
                    if (!minIndexes.contains(Integer.valueOf(i))) {
                        rates[i] = amounts[i].floatValue() / total.floatValue() * restRate;
                    }
                }
            }

            for (int i = 0;
                 i < amounts.length;
                 i++) {
                float sweepAngle = rates[i] * totalAngle;
                paint.setColor(Colors[i]);
                canvas.drawArc(rect, angle, sweepAngle, true, paint);
                angle += sweepAngle;
            }
        } else {
            paint.setColor(Colors[Colors.length - 1]);
            canvas.drawArc(rect, angle, totalAngle, true, paint);
        }
        super.onDraw(canvas);
    }

    public void causeDraw() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }


    // rotation start
    double fingerRotation;
    double newFingerRotation;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        final float xc = getWidth() / 2;
        final float yc = getHeight() / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setStartAngleRound360(getStartAngle());
                fingerRotation = Math.toDegrees(Math.atan2(x - xc, yc - y));
                inertia.beginCollectSpeed();
                break;
            case MotionEvent.ACTION_MOVE:
                newFingerRotation = Math.toDegrees(Math.atan2(x - xc, yc - y));
                double rotationDelta = newFingerRotation - fingerRotation;
                if (Math.abs(rotationDelta) > 360.0 / 4) {
                    break;
                }
                setStartAngle((float) (getStartAngle() + rotationDelta));
                fingerRotation = newFingerRotation;
                break;
            case MotionEvent.ACTION_UP:
                fingerRotation = newFingerRotation = 0.0f;
                inertia.endCollectSpeed();
                break;
        }
        return true;
    }

    private Inertia inertia = new Inertia();

    private class Inertia {
        private final int SpeedCollectionDuration = 60;
        private final float InertiaResistance = 0.001f;
        private final int InertiaDrawInterval = 20;

        private ArrayList<TimeAndRotation> rotations = new ArrayList<TimeAndRotation>();

        public void beginCollectSpeed() {
            reset();
            collectRotationRunnable.run();
        }

        public void endCollectSpeed() {
            removeCallbacks(collectRotationRunnable);
            removeCallbacks(inertiaDrawRunnable);
            TimeAndRotation currentRotation = new TimeAndRotation();
            if (rotations.size() > 0) {
                TimeAndRotation lastRotation = null;
                for (int i = rotations.size() - 1;
                     i >= 0;
                     i--) {
                    TimeAndRotation r = rotations.get(i);
                    if (currentRotation.timeStamp - r.timeStamp >= SpeedCollectionDuration) {
                        lastRotation = r;
                        break;
                    }
                }
                rotations.clear();
                if (lastRotation != null) {
                    handleInertia(currentRotation, lastRotation);
                    return;
                }
            }
            setStartAngle(getStartAngle());
        }

        public void reset() {
            rotations.clear();
            removeCallbacks(collectRotationRunnable);
            removeCallbacks(inertiaDrawRunnable);
            inertiaSpeed = 0;
            lastInertialDrawTime = System.currentTimeMillis();
        }

        private Runnable collectRotationRunnable = new Runnable() {
            @Override
            public void run() {
                removeCallbacks(collectRotationRunnable);
                TimeAndRotation cr = new TimeAndRotation();
                for (int i = rotations.size() - 1;
                     i >= 0;
                     i--) {
                    TimeAndRotation r = rotations.get(i);
                    if (cr.timeStamp - r.timeStamp > SpeedCollectionDuration * 1.5f) {
                        rotations.remove(r);
                    }
                }
                rotations.add(cr);
                postDelayed(collectRotationRunnable, SpeedCollectionDuration);
            }
        };

        private class TimeAndRotation {
            public long timeStamp;
            public float rotation;

            public TimeAndRotation() {
                timeStamp = System.currentTimeMillis();
                rotation = getStartAngle();
            }
        }

        // handle inertial draw begin
        private float inertiaSpeed;
        private long lastInertialDrawTime;

        private void handleInertia(TimeAndRotation currentRotation, TimeAndRotation lastRotation) {
            removeCallbacks(inertiaDrawRunnable);
            inertiaSpeed = (currentRotation.rotation - lastRotation.rotation) / (float)
                    (currentRotation.timeStamp - lastRotation.timeStamp);
            lastInertialDrawTime = System.currentTimeMillis();
            postDelayed(inertiaDrawRunnable, InertiaDrawInterval);
        }

        private Runnable inertiaDrawRunnable = new Runnable() {
            @Override
            public void run() {
                removeCallbacks(inertiaDrawRunnable);
                if (Math.abs(inertiaSpeed) < InertiaResistance * (float) InertiaDrawInterval) {
                    return;
                }
                long drawTime = System.currentTimeMillis();
                long duration = drawTime - lastInertialDrawTime;
                float angleDelta = inertiaSpeed * duration;
                setStartAngle(getStartAngle() + angleDelta);
                float speedDelta = InertiaResistance * (duration);
                if (inertiaSpeed > 0) {
                    if (inertiaSpeed > speedDelta) {
                        inertiaSpeed -= speedDelta;
                        lastInertialDrawTime = drawTime;
                        postDelayed(inertiaDrawRunnable, InertiaDrawInterval);
                    }
                } else {
                    if (inertiaSpeed < speedDelta) {
                        inertiaSpeed += speedDelta;
                        lastInertialDrawTime = drawTime;
                        postDelayed(inertiaDrawRunnable, InertiaDrawInterval);
                    }
                }
            }
        };

        // handle inertial draw end
    }

    // rotation end

    public void setRotateListener(RotateListener listener) {
        rotateListener = listener;
        notifyRotateListener();
    }

    private void notifyRotateListener() {
        if (rotateListener != null) {
            rotateListener.onRotationChanged(getStartAngle() - DefaultStartAngle);
        }
    }

    public Drawable getSymbolForIndex(int index) {
        int color = Colors[0];
        if (index >= 0 && index < Colors.length) {
            color = Colors[index];
        }
        return new ShapeDrawable(new RoundShape(color));
    }

    private class RoundShape extends Shape {
        private int color;

        public RoundShape(int color) {
            this.color = color;
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawOval(new RectF(2, 2, canvas.getWidth() - 2, canvas.getHeight() - 2), paint);
        }
    }
}
