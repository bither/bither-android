/*
 * MinusStickChart.java
 * Android-Charts
 *
 * Created by limc on 2011/05/29.
 *
 * Copyright 2011 limc.cn All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.charts.view;

import net.bither.charts.entity.IMeasurable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

public class MinusStickChart extends StickChart {

    public static final int DEFAULT_STICK_SPACING = 6;

    private int stickSpacing = DEFAULT_STICK_SPACING;

    public MinusStickChart(Context context) {
        super(context);
    }

    public MinusStickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MinusStickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void calcValueRangePaddingZero() {
    }

    @Override
    protected void calcValueRangeFormatForAxis() {
    }

    @Override
    protected void drawSticks(Canvas canvas) {
        if (null == stickData) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }

        Paint mPaintFill = new Paint();
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setColor(super.getStickFillColor());

        Paint mPaintBorder = new Paint();
        mPaintBorder.setStyle(Style.STROKE);
        mPaintBorder.setStrokeWidth(2);
        mPaintBorder.setColor(super.getStickBorderColor());

        float stickWidth = getDataQuadrantPaddingWidth() / maxSticksNum
                - stickSpacing;

        if (axisYPosition == AXIS_Y_POSITION_LEFT) {

            float stickX = getDataQuadrantPaddingStartX();
            for (int i = 0;
                 i < stickData.size();
                 i++) {
                IMeasurable entity = stickData.get(i);

                float highY = (float) ((1f - (entity.getHigh() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float lowY = (float) ((1f - (entity.getLow() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                // draw stick
                canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                        mPaintFill);
                canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                        mPaintBorder);

                // next x
                stickX = stickX + stickSpacing + stickWidth;
            }
        } else {

            float stickX = getDataQuadrantPaddingEndX() - stickWidth;
            for (int i = stickData.size() - 1;
                 i >= 0;
                 i--) {
                IMeasurable stick = stickData.get(i);

                float highY = (float) ((1f - (stick.getHigh() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float lowY = (float) ((1f - (stick.getLow() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                // draw stick
                canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                        mPaintFill);
                canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                        mPaintBorder);

                // next x
                stickX = stickX - stickSpacing - stickWidth;
            }
        }
    }

    public int getStickSpacing() {
        return stickSpacing;
    }

    public void setStickSpacing(int stickSpacing) {
        this.stickSpacing = stickSpacing;
    }
}
