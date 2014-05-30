/*
 * CandleStickChart.java
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

import net.bither.charts.R;
import net.bither.charts.entity.IMeasurable;
import net.bither.charts.entity.OHLCEntity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class CandleStickChart extends StickChart {

    public static final int DEFAULT_POSITIVE_STICK_BORDER_COLOR = Color.RED;

    public final int DEFAULT_POSITIVE_STICK_FILL_COLOR = getContext()
            .getResources().getColor(R.drawable.k_line_red);
    public static final int DEFAULT_NEGATIVE_STICK_BORDER_COLOR = Color.GREEN;

    public final int DEFAULT_NEGATIVE_STICK_FILL_COLOR = getContext()
            .getResources().getColor(R.drawable.k_line_green);
    ;

    public static final int DEFAULT_CROSS_STAR_COLOR = Color.LTGRAY;

    private int positiveStickBorderColor = DEFAULT_POSITIVE_STICK_BORDER_COLOR;

    private int positiveStickFillColor = DEFAULT_POSITIVE_STICK_FILL_COLOR;

    private int negativeStickBorderColor = DEFAULT_NEGATIVE_STICK_BORDER_COLOR;

    private int negativeStickFillColor = DEFAULT_NEGATIVE_STICK_FILL_COLOR;

    private int crossStarColor = DEFAULT_CROSS_STAR_COLOR;

    public CandleStickChart(Context context) {
        super(context);
    }

    public CandleStickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CandleStickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void calcDataValueRange() {
        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;
        IMeasurable first;
        if (axisYPosition == AXIS_Y_POSITION_LEFT) {
            first = this.stickData.get(0);
        } else {
            first = this.stickData.get(this.stickData.size() - 1);
        }
        if (first.getHigh() == 0 && first.getLow() == 0) {

        } else {
            maxValue = first.getHigh();
            minValue = first.getLow();
        }
        for (int i = 0;
             i < this.maxSticksNum;
             i++) {
            OHLCEntity stick;
            if (axisYPosition == AXIS_Y_POSITION_LEFT) {
                stick = (OHLCEntity) this.stickData.get(i);
            } else {
                stick = (OHLCEntity) this.stickData.get(this.stickData.size()
                        - 1 - i);
            }

            if (stick.getOpen() == 0 && stick.getHigh() == 0
                    && stick.getLow() == 0) {
                if (stick.getClose() > 0) {
                    if (stick.getClose() < minValue) {
                        minValue = stick.getClose();
                    }

                    if (stick.getClose() > maxValue) {
                        maxValue = stick.getClose();
                    }
                }
            } else {
                if (stick.getLow() < minValue) {
                    minValue = stick.getLow();
                }

                if (stick.getHigh() > maxValue) {
                    maxValue = stick.getHigh();
                }
            }
        }

        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void drawSticks(Canvas canvas) {
        if (null == stickData) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }

        Paint mPaintPositive = new Paint();
        mPaintPositive.setColor(positiveStickFillColor);

        Paint mPaintNegative = new Paint();
        mPaintNegative.setColor(negativeStickFillColor);

        Paint mPaintCross = new Paint();
        mPaintCross.setColor(crossStarColor);

        float stickWidth = getDataQuadrantPaddingWidth() / maxSticksNum
                - stickSpacing;

        if (axisYPosition == AXIS_Y_POSITION_LEFT) {
            float stickX = getDataQuadrantPaddingStartX();

            for (int i = 0;
                 i < stickData.size();
                 i++) {
                OHLCEntity ohlc = (OHLCEntity) stickData.get(i);
                float openY = (float) ((1f - (ohlc.getOpen() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float highY = (float) ((1f - (ohlc.getHigh() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float lowY = (float) ((1f - (ohlc.getLow() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float closeY = (float) ((1f - (ohlc.getClose() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                if (ohlc.getOpen() < ohlc.getClose()) {
                    // stick or line
                    if (stickWidth >= 2f) {
                        canvas.drawRect(stickX, closeY, stickX + stickWidth,
                                openY, mPaintPositive);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintPositive);
                } else if (ohlc.getOpen() > ohlc.getClose()) {
                    // stick or line
                    if (stickWidth >= 2f) {
                        canvas.drawRect(stickX, openY, stickX + stickWidth,
                                closeY, mPaintNegative);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintNegative);
                } else {
                    // line or point
                    if (stickWidth >= 2f) {
                        canvas.drawLine(stickX, closeY, stickX + stickWidth,
                                openY, mPaintCross);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintCross);
                }

                // next x
                stickX = stickX + stickSpacing + stickWidth;
            }
        } else {
            float stickX = getDataQuadrantPaddingEndX() - stickWidth;
            for (int i = stickData.size() - 1;
                 i >= 0;
                 i--) {
                OHLCEntity ohlc = (OHLCEntity) stickData.get(i);
                float openY = (float) ((1f - (ohlc.getOpen() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float highY = (float) ((1f - (ohlc.getHigh() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float lowY = (float) ((1f - (ohlc.getLow() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float closeY = (float) ((1f - (ohlc.getClose() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                if (ohlc.getOpen() < ohlc.getClose()) {
                    // stick or line
                    if (stickWidth >= 2f) {
                        canvas.drawRect(stickX, closeY, stickX + stickWidth,
                                openY, mPaintPositive);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintPositive);
                } else if (ohlc.getOpen() > ohlc.getClose()) {
                    // stick or line
                    if (stickWidth >= 2f) {
                        canvas.drawRect(stickX, openY, stickX + stickWidth,
                                closeY, mPaintNegative);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintNegative);
                } else {
                    // line or point
                    if (stickWidth >= 2f) {
                        canvas.drawLine(stickX, closeY, stickX + stickWidth,
                                openY, mPaintCross);
                    }
                    canvas.drawLine(stickX + stickWidth / 2f, highY, stickX
                            + stickWidth / 2f, lowY, mPaintCross);
                }

                // next x
                stickX = stickX - stickSpacing - stickWidth;
            }
        }
    }

    public int getPositiveStickBorderColor() {
        return positiveStickBorderColor;
    }

    public void setPositiveStickBorderColor(int positiveStickBorderColor) {
        this.positiveStickBorderColor = positiveStickBorderColor;
    }

    public int getPositiveStickFillColor() {
        return positiveStickFillColor;
    }

    public void setPositiveStickFillColor(int positiveStickFillColor) {
        this.positiveStickFillColor = positiveStickFillColor;
    }

    public int getNegativeStickBorderColor() {
        return negativeStickBorderColor;
    }

    public void setNegativeStickBorderColor(int negativeStickBorderColor) {
        this.negativeStickBorderColor = negativeStickBorderColor;
    }

    public int getNegativeStickFillColor() {
        return negativeStickFillColor;
    }

    public void setNegativeStickFillColor(int negativeStickFillColor) {
        this.negativeStickFillColor = negativeStickFillColor;
    }

    public int getCrossStarColor() {
        return crossStarColor;
    }

    public void setCrossStarColor(int crossStarColor) {
        this.crossStarColor = crossStarColor;
    }


}
