/*
 * StickChart.java
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

import java.util.ArrayList;
import java.util.List;

import net.bither.charts.entity.IChartData;
import net.bither.charts.entity.IMeasurable;
import net.bither.charts.entity.IStickEntity;
import net.bither.charts.entity.StickEntity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;

public class StickChart extends GridChart {

    public static final int DEFAULT_STICK_BORDER_COLOR = Color.RED;

    public static final int DEFAULT_STICK_FILL_COLOR = Color.RED;

    private int stickBorderColor = DEFAULT_STICK_BORDER_COLOR;

    private int stickFillColor = DEFAULT_STICK_FILL_COLOR;

    public static final boolean DEFAULT_AUTO_CALC_VALUE_RANGE = true;
    public static final int DEFAULT_STICK_SPACING = 1;
    protected IChartData<IStickEntity> stickData;

    protected int maxSticksNum;

    protected double maxValue;

    protected double minValue;

    protected boolean autoCalcValueRange = DEFAULT_AUTO_CALC_VALUE_RANGE;

    protected int stickSpacing = DEFAULT_STICK_SPACING;

    public StickChart(Context context) {
        super(context);
    }

    public StickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void calcDataValueRange() {
        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;
        IMeasurable first;
        if (axisYPosition == AXIS_Y_POSITION_LEFT) {
            first = this.stickData.get(0);
        } else {
            first = this.stickData.get(stickData.size() - 1);
        }
        if (first.getHigh() == 0 && first.getLow() == 0) {

        } else {
            maxValue = first.getHigh();
            minValue = first.getLow();
        }

        for (int i = 0;
             i < this.maxSticksNum;
             i++) {
            IMeasurable stick;
            if (axisYPosition == AXIS_Y_POSITION_LEFT) {
                stick = this.stickData.get(i);
            } else {
                stick = this.stickData.get(stickData.size() - 1 - i);
            }
            if (stick.getLow() < minValue) {
                minValue = stick.getLow();
            }

            if (stick.getHigh() > maxValue) {
                maxValue = stick.getHigh();
            }

        }

        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    protected void calcValueRangePaddingZero() {
        double maxValue = this.maxValue;
        double minValue = this.minValue;

        if ((long) maxValue > (long) minValue) {
            if ((maxValue - minValue) < 10 && minValue > 1) {
                this.maxValue = (long) (maxValue + 1);
                this.minValue = (long) (minValue - 1);
            } else {
                this.maxValue = (long) (maxValue + (maxValue - minValue) * 0.1);
                this.minValue = (long) (minValue - (maxValue - minValue) * 0.1);
                if (this.minValue < 0) {
                    this.minValue = 0;
                }
            }
        } else if ((long) maxValue == (long) minValue) {
            if (maxValue <= 10 && maxValue > 1) {
                this.maxValue = maxValue + 1;
                this.minValue = minValue - 1;
            } else if (maxValue <= 100 && maxValue > 10) {
                this.maxValue = maxValue + 10;
                this.minValue = minValue - 10;
            } else if (maxValue <= 1000 && maxValue > 100) {
                this.maxValue = maxValue + 100;
                this.minValue = minValue - 100;
            } else if (maxValue <= 10000 && maxValue > 1000) {
                this.maxValue = maxValue + 1000;
                this.minValue = minValue - 1000;
            } else if (maxValue <= 100000 && maxValue > 10000) {
                this.maxValue = maxValue + 10000;
                this.minValue = minValue - 10000;
            } else if (maxValue <= 1000000 && maxValue > 100000) {
                this.maxValue = maxValue + 100000;
                this.minValue = minValue - 100000;
            } else if (maxValue <= 10000000 && maxValue > 1000000) {
                this.maxValue = maxValue + 1000000;
                this.minValue = minValue - 1000000;
            } else if (maxValue <= 100000000 && maxValue > 10000000) {
                this.maxValue = maxValue + 10000000;
                this.minValue = minValue - 10000000;
            }
        } else {
            this.maxValue = 0;
            this.minValue = 0;
        }

    }

    protected void calcValueRangeFormatForAxis() {
        long rate = (long) (this.maxValue - this.minValue) / (this.latitudeNum);
        String strRate = String.valueOf(rate);
        float first = Integer.parseInt(String.valueOf(strRate.charAt(0))) + 1.0f;
        if (first > 0 && strRate.length() > 1) {
            float second = Integer.parseInt(String.valueOf(strRate.charAt(1)));
            if (second < 5) {
                first = first - 0.5f;
            }
            rate = (long) (first * Math.pow(10, strRate.length() - 1));
        } else {
            rate = 1;
        }
        if (this.latitudeNum > 0
                && (long) (this.maxValue - this.minValue)
                % (this.latitudeNum * rate) != 0) {
            this.maxValue = (long) this.maxValue
                    + (this.latitudeNum * rate)
                    - ((long) (this.maxValue - this.minValue) % (this.latitudeNum * rate));
        }
    }

    protected void calcValueRange() {
        if (this.stickData != null && this.stickData.size() > 0) {
            this.calcDataValueRange();
            this.calcValueRangePaddingZero();
        } else {
            this.maxValue = 0;
            this.minValue = 0;
        }
        this.calcValueRangeFormatForAxis();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (this.autoCalcValueRange) {
            calcValueRange();
        }
        initAxisY();
        initAxisX();
        super.onDraw(canvas);

        drawSticks(canvas);
    }

    @Override
    public String getAxisXGraduate(Object value) {
        float graduate = Float.valueOf(super.getAxisXGraduate(value));
        int index = (int) Math.floor(graduate * maxSticksNum);

        if (index >= maxSticksNum) {
            index = maxSticksNum - 1;
        } else if (index < 0) {
            index = 0;
        }

        return String.valueOf(stickData.get(index).getDate());
    }

    @Override
    public String getAxisYGraduate(Object value) {
        float graduate = Float.valueOf(super.getAxisYGraduate(value));
        return String.valueOf((int) Math.floor(graduate * (maxValue - minValue)
                + minValue));
    }

    @Override
    public void notifyEvent(GridChart chart) {
        CandleStickChart candlechart = (CandleStickChart) chart;

        this.maxSticksNum = candlechart.getMaxSticksNum();

        // notifyEvent
        super.notifyEvent(chart);
        // notifyEventAll

    }

    protected void initAxisX() {
        List<String> titleX = new ArrayList<String>();
        if (null != stickData && stickData.size() > 0) {
            float average = maxSticksNum / this.getLongitudeNum();
            for (int i = 0;
                 i < this.getLongitudeNum();
                 i++) {
                int index = (int) Math.floor(i * average);
                if (index > maxSticksNum - 1) {
                    index = maxSticksNum - 1;
                }
                titleX.add(String.valueOf(stickData.get(index).getTitle()));
            }
            titleX.add(String.valueOf(stickData.get(maxSticksNum - 1)
                    .getTitle()));
        }
        super.setLongitudeTitles(titleX);
    }

    public int getSelectedIndex() {
        if (null == super.getTouchPoint()) {
            return 0;
        }
        float graduate = Float.valueOf(super.getAxisXGraduate(super
                .getTouchPoint().x));
        int index = (int) Math.floor(graduate * maxSticksNum);

        if (index >= maxSticksNum) {
            index = maxSticksNum - 1;
        } else if (index < 0) {
            index = 0;
        }

        return index;
    }

    protected void initAxisY() {
        List<String> titleY = new ArrayList<String>();
        float average = (int) ((maxValue - minValue) / this.getLatitudeNum())
                / getGraduation() * getGraduation();
        ;
        // calculate degrees on Y axis
        for (int i = 0;
             i < this.getLatitudeNum();
             i++) {
            String value = String.valueOf((int) Math.floor(minValue + i
                    * average));
            if (value.length() < super.getLatitudeMaxTitleLength()) {
                while (value.length() < super.getLatitudeMaxTitleLength()) {
                    value = " " + value;
                }
            }
            titleY.add(value);
        }
        // calculate last degrees by use max value
        String value = String.valueOf((int) Math.floor(((int) maxValue)
                / getGraduation() * getGraduation()));
        if (value.length() < super.getLatitudeMaxTitleLength()) {
            while (value.length() < super.getLatitudeMaxTitleLength()) {
                value = " " + value;
            }
        }
        titleY.add(value);

        super.setLatitudeTitles(titleY);
    }

    protected void drawSticks(Canvas canvas) {
        if (null == stickData) {
            return;
        }
        if (stickData.size() <= 0) {
            return;
        }

        Paint mPaintStick = new Paint();
        mPaintStick.setColor(stickFillColor);

        float stickWidth = getDataQuadrantPaddingWidth() / maxSticksNum
                - stickSpacing;

        if (axisYPosition == AXIS_Y_POSITION_LEFT) {

            float stickX = getDataQuadrantPaddingStartX();

            for (int i = 0;
                 i < stickData.size();
                 i++) {
                IMeasurable stick = stickData.get(i);

                float highY = (float) ((1f - (stick.getHigh() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
                float lowY = (float) ((1f - (stick.getLow() - minValue)
                        / (maxValue - minValue))
                        * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

                if (stickWidth >= 2f) {
                    canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                            mPaintStick);
                } else {
                    canvas.drawLine(stickX, highY, stickX, lowY, mPaintStick);
                }

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

                if (stickWidth >= 2f) {
                    canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                            mPaintStick);
                } else {
                    canvas.drawLine(stickX, highY, stickX, lowY, mPaintStick);
                }

                // next x
                stickX = stickX - stickSpacing - stickWidth;
            }
        }

    }

    public void pushData(StickEntity entity) {
        if (null != entity) {
            addData(entity);
            super.postInvalidate();
        }
    }

    public void addData(StickEntity entity) {
        if (null != entity) {
            // add
            this.stickData.add(entity);

            if (this.maxValue < entity.getHigh()) {
                this.maxValue = ((int) entity.getHigh()) / getGraduation()
                        * getGraduation();
            }

            if (this.maxValue < entity.getLow()) {
                this.minValue = ((int) entity.getLow()) / getGraduation()
                        * getGraduation();
            }

            if (stickData.size() > maxSticksNum) {
                maxSticksNum = maxSticksNum + 1;
            }
        }
    }

    private final int NONE = 0;
    private final int ZOOM = 1;
    private final int DOWN = 2;

    private float olddistance = 0f;
    private float newdistance = 0f;

    private int touchMode;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final float MIN_LENGTH = (super.getWidth() / 40) < 5 ? 5 : (super
                .getWidth() / 50);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchMode = DOWN;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                touchMode = NONE;
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                olddistance = calcDistance(event);
                if (olddistance > MIN_LENGTH) {
                    touchMode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchMode == ZOOM) {
                    newdistance = calcDistance(event);
                    if (newdistance > MIN_LENGTH
                            && Math.abs(newdistance - olddistance) > MIN_LENGTH) {

                        if (newdistance > olddistance) {
                            zoomIn();
                        } else {
                            zoomOut();
                        }
                        olddistance = newdistance;

                        super.postInvalidate();

                    }
                }
                break;
        }
        return true;
    }

    private float calcDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    protected void zoomIn() {
        if (maxSticksNum > 10) {
            maxSticksNum = maxSticksNum - 3;
        }
    }

    protected void zoomOut() {
        if (maxSticksNum < stickData.size() - 1 - 3) {
            maxSticksNum = maxSticksNum + 3;
        }
    }

    public int getStickBorderColor() {
        return stickBorderColor;
    }

    public void setStickBorderColor(int stickBorderColor) {
        this.stickBorderColor = stickBorderColor;
    }

    public int getStickFillColor() {
        return stickFillColor;
    }

    public void setStickFillColor(int stickFillColor) {
        this.stickFillColor = stickFillColor;
    }

    public IChartData<IStickEntity> getStickData() {
        return stickData;
    }

    public void setStickData(IChartData<IStickEntity> stickData) {
        this.stickData = stickData;
    }

    public int getMaxSticksNum() {
        return maxSticksNum;
    }

    public void setMaxSticksNum(int maxSticksNum) {
        this.maxSticksNum = maxSticksNum;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public boolean isAutoCalcValueRange() {
        return autoCalcValueRange;
    }

    public void setAutoCalcValueRange(boolean autoCalcValueRange) {
        this.autoCalcValueRange = autoCalcValueRange;
    }
}
