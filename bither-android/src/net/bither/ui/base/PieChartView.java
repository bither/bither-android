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
import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by songchenwen on 14-6-8.
 */
public class PieChartView extends View {
    public static final int[] Colors = new int[]{Color.rgb(254, 35, 93), Color.rgb(36, 182, 212),
            Color.rgb(38, 230, 91), Color.rgb(194, 253, 70), Color.rgb(237, 193, 155),
            Color.rgb(100, 114, 253), Color.rgb(117, 140, 129), Color.rgb(200, 47, 217),
            Color.rgb(239, 204, 41), Color.rgb(253, 38, 38), Color.rgb(253, 160, 38),
            Color.rgb(144, 183, 177)};
    private static final int TransformDuration = 400;
    private static final Paint paint = new Paint();
    private static final float MaxTotalAngle = 360;
    private static final float MinRate = 0.05f;

    static {
        paint.setAntiAlias(true);
    }

    private BigInteger[] amounts;
    private BigInteger total;
    private float startAngle = 90;

    private float totalAngle = 0;

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAmounts(BigInteger[] amounts) {
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

    public void setAmount(BigInteger... amounts) {
        setAmounts(amounts);
    }

    public void setStartAngle(float angle) {
        this.startAngle = angle;
        causeDraw();
    }

    public float getStartAngle() {
        return startAngle;
    }

    public void setTotalAngle(float total) {
        if (total <= MaxTotalAngle) {
            totalAngle = total;
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
                if (rates[i] < MinRate) {
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
