/*
 * MACDChart.java
 * Android-Charts
 *
 * Created by limc on 2014.
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
import net.bither.charts.entity.MACDEntity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;

public class MACDChart extends SlipStickChart {

    public static final int MACD_DISPLAY_TYPE_STICK = 1 << 0;
    public static final int MACD_DISPLAY_TYPE_LINE = 1 << 1;
    public static final int MACD_DISPLAY_TYPE_LINE_STICK = 1 << 2;

    public static final int DEFAULT_POSITIVE_STICK_COLOR = Color.RED;
    public static final int DEFAULT_NEGATIVE_STICK_COLOR = Color.BLUE;
    public static final int DEFAULT_MACD_LINE_COLOR = Color.RED;
    public static final int DEFAULT_DIFF_LINE_COLOR = Color.WHITE;
    public static final int DEFAULT_DEA_LINE_COLOR = Color.YELLOW;
    public static final int DEFAULT_MACD_DISPLAY_TYPE = MACD_DISPLAY_TYPE_LINE_STICK;

    private int positiveStickColor = DEFAULT_POSITIVE_STICK_COLOR;
    private int negativeStickColor = DEFAULT_NEGATIVE_STICK_COLOR;
    private int macdLineColor = DEFAULT_MACD_LINE_COLOR;
    private int diffLineColor = DEFAULT_DIFF_LINE_COLOR;
    private int deaLineColor = DEFAULT_DEA_LINE_COLOR;
    private int macdDisplayType = DEFAULT_MACD_DISPLAY_TYPE;

    public MACDChart(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public MACDChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MACDChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void calcValueRange() {
        if (stickData == null) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }
        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;

        IMeasurable first = stickData.get(displayFrom);
        maxValue = Math.max(first.getHigh(), maxValue);
        minValue = Math.min(first.getLow(), minValue);
        // 判断显示为方柱或显示为线条
        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            IMeasurable macd = stickData.get(i);
            maxValue = Math.max(macd.getHigh(), maxValue);
            minValue = Math.min(macd.getLow(), minValue);
        }
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLinesData(canvas);
    }

    @Override
    protected void drawSticks(Canvas canvas) {

        if (stickData == null) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }

        if (macdDisplayType == MACD_DISPLAY_TYPE_LINE) {
            this.drawMacdLine(canvas);
            return;
        }

        Paint mPaintStick = new Paint();
        mPaintStick.setAntiAlias(true);

        float stickWidth = getDataQuadrantPaddingWidth() / displayNumber
                - stickSpacing;
        float stickX = getDataQuadrantPaddingStartX();
        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            MACDEntity stick = (MACDEntity) stickData.get(i);

            float highY;
            float lowY;
            if (stick.getMacd() == 0) {
                continue;
            }
            if (stick.getMacd() > 0) {
                mPaintStick.setColor(positiveStickColor);
                highY = (float) ((1 - (stick.getMacd() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                lowY = (float) ((1 - (0 - minValue) / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

            } else {
                mPaintStick.setColor(negativeStickColor);
                highY = (float) ((1 - (0 - minValue) / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                lowY = (float) ((1 - (stick.getMacd() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

            }

            if (macdDisplayType == MACD_DISPLAY_TYPE_STICK) {
                if (stickWidth >= 2) {
                    canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                            mPaintStick);
                } else {
                    canvas.drawLine(stickX, highY, stickX, lowY, mPaintStick);
                }
            } else if (macdDisplayType == MACD_DISPLAY_TYPE_LINE_STICK) {
                canvas.drawLine(stickX + stickWidth / 2, highY, stickX
                        + stickWidth / 2, lowY, mPaintStick);
            }
            stickX = stickX + stickSpacing + stickWidth;
        }
    }

    protected void drawDiffLine(Canvas canvas) {

        if (null == this.stickData) {
            return;
        }
        Paint mPaintStick = new Paint();
        mPaintStick.setAntiAlias(true);
        mPaintStick.setColor(diffLineColor);

        // distance between two points
        float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
        // start point‘s X
        float startX = getDataQuadrantPaddingStartX() + lineLength / 2;
        // start point
        PointF ptFirst = null;
        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            MACDEntity entity = (MACDEntity) stickData.get(i);
            // calculate Y
            float valueY = (float) ((1f - (entity.getDiff() - minValue)
                    / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                    + getDataQuadrantPaddingStartY();

            // if is not last point connect to previous point
            if (i > displayFrom) {
                canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                        mPaintStick);
            }
            // reset
            ptFirst = new PointF(startX, valueY);
            startX = startX + 1 + lineLength;
        }
    }

    protected void drawDeaLine(Canvas canvas) {

        Paint mPaintStick = new Paint();
        mPaintStick.setAntiAlias(true);
        mPaintStick.setColor(deaLineColor);
        // distance between two points
        float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
        // set start point’s X
        float startX = getDataQuadrantPaddingStartX() + lineLength / 2;
        // start point
        PointF ptFirst = null;
        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            MACDEntity entity = (MACDEntity) stickData.get(i);
            // calculate Y
            float valueY = (float) ((1f - (entity.getDea() - minValue)
                    / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                    + getDataQuadrantPaddingStartY();

            // if is not last point connect to previous point
            if (i > displayFrom) {
                canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                        mPaintStick);
            }
            // reset
            ptFirst = new PointF(startX, valueY);
            startX = startX + 1 + lineLength;
        }
    }

    protected void drawMacdLine(Canvas canvas) {
        Paint mPaintStick = new Paint();
        mPaintStick.setAntiAlias(true);
        mPaintStick.setColor(macdLineColor);

        // distance between two points
        float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
        // set start point’s X
        float startX = getDataQuadrantPaddingStartX() + lineLength / 2;
        // start point
        PointF ptFirst = null;
        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            MACDEntity entity = (MACDEntity) stickData.get(i);
            // calculate Y
            float valueY = (float) ((1f - (entity.getMacd() - minValue)
                    / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                    + getDataQuadrantPaddingStartY();

            // if is not last point connect to previous point
            if (i > displayFrom) {
                canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                        mPaintStick);
            }
            // reset
            ptFirst = new PointF(startX, valueY);
            startX = startX + 1 + lineLength;
        }
    }

    protected void drawLinesData(Canvas canvas) {

        if (stickData == null) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }

        drawDeaLine(canvas);
        drawDiffLine(canvas);
    }

    public int getPositiveStickColor() {
        return positiveStickColor;
    }

    public void setPositiveStickColor(int positiveStickColor) {
        this.positiveStickColor = positiveStickColor;
    }

    public int getNegativeStickColor() {
        return negativeStickColor;
    }

    public void setNegativeStickColor(int negativeStickColor) {
        this.negativeStickColor = negativeStickColor;
    }

    public int getMacdLineColor() {
        return macdLineColor;
    }

    public void setMacdLineColor(int macdLineColor) {
        this.macdLineColor = macdLineColor;
    }

    public int getDiffLineColor() {
        return diffLineColor;
    }

    public void setDiffLineColor(int diffLineColor) {
        this.diffLineColor = diffLineColor;
    }

    public int getDeaLineColor() {
        return deaLineColor;
    }

    public void setDeaLineColor(int deaLineColor) {
        this.deaLineColor = deaLineColor;
    }

    public int getMacdDisplayType() {
        return macdDisplayType;
    }

    public void setMacdDisplayType(int macdDisplayType) {
        this.macdDisplayType = macdDisplayType;
    }

}
