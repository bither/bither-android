/*
 * SlipStickChart.java
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
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;

public class SlipStickChart extends GridChart {

    public static final int ZOOM_BASE_LINE_CENTER = 0;
    public static final int ZOOM_BASE_LINE_LEFT = 1;
    public static final int ZOOM_BASE_LINE_RIGHT = 2;

    public static final int DEFAULT_DISPLAY_FROM = 0;
    public static final int DEFAULT_DISPLAY_NUMBER = 50;
    public static final int DEFAULT_MIN_DISPLAY_NUMBER = 20;
    public static final int DEFAULT_ZOOM_BASE_LINE = ZOOM_BASE_LINE_CENTER;
    public static final boolean DEFAULT_AUTO_CALC_VALUE_RANGE = true;

    public static final int DEFAULT_STICK_SPACING = 1;

    protected int displayFrom = DEFAULT_DISPLAY_FROM;
    protected int displayNumber = DEFAULT_DISPLAY_NUMBER;
    protected int minDisplayNumber = DEFAULT_MIN_DISPLAY_NUMBER;
    protected int zoomBaseLine = DEFAULT_ZOOM_BASE_LINE;
    protected boolean autoCalcValueRange = DEFAULT_AUTO_CALC_VALUE_RANGE;
    protected int stickSpacing = DEFAULT_STICK_SPACING;
    public static final int DEFAULT_STICK_BORDER_COLOR = Color.RED;

    public static final int DEFAULT_STICK_FILL_COLOR = Color.RED;

    private int stickBorderColor = DEFAULT_STICK_BORDER_COLOR;

    private int stickFillColor = DEFAULT_STICK_FILL_COLOR;

    protected IChartData<IStickEntity> stickData;

    protected double maxValue;

    protected double minValue;

    public SlipStickChart(Context context) {
        super(context);
    }

    public SlipStickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SlipStickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void calcDataValueRange() {

        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;

        IMeasurable first = this.stickData.get(0);
        if (first.getHigh() == 0 && first.getLow() == 0) {

        } else {
            maxValue = first.getHigh();
            minValue = first.getLow();
        }

        for (int i = this.displayFrom;
             i < this.displayFrom
                     + this.displayNumber;
             i++) {
            IMeasurable stick = this.stickData.get(i);
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
        if (null == this.stickData) {
            this.maxValue = 0;
            this.minValue = 0;
            return;
        }

        if (this.stickData.size() > 0) {
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
        int index = (int) Math.floor(graduate * displayNumber);

        if (index >= displayNumber) {
            index = displayNumber - 1;
        } else if (index < 0) {
            index = 0;
        }

        index = index + displayFrom;

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
        // CandleStickChart candlechart = (CandleStickChart) chart;
        // this. = candlechart.getMaxSticksNum();

        // notifyEvent
        super.notifyEvent(chart);
        ;
    }

    protected void initAxisX() {
        List<String> titleX = new ArrayList<String>();
        if (null != stickData && stickData.size() > 0) {
            float average = displayNumber / this.getLongitudeNum();
            for (int i = 0;
                 i < this.getLongitudeNum();
                 i++) {
                int index = (int) Math.floor(i * average);
                if (index > displayNumber - 1) {
                    index = displayNumber - 1;
                }
                index = index + displayFrom;
                titleX.add(String.valueOf(stickData.get(index).getDate())
                        .substring(4));
            }
            titleX.add(String.valueOf(
                    stickData.get(displayFrom + displayNumber - 1).getDate())
                    .substring(4));
        }
        super.setLongitudeTitles(titleX);
    }

    public int getSelectedIndex() {
        if (null == super.getTouchPoint()) {
            return 0;
        }
        float graduate = Float.valueOf(super.getAxisXGraduate(super
                .getTouchPoint().x));
        int index = (int) Math.floor(graduate * displayNumber);

        if (index >= displayNumber) {
            index = displayNumber - 1;
        } else if (index < 0) {
            index = 0;
        }
        index = index + displayFrom;
        return index;
    }

    protected void initAxisY() {
        this.calcValueRange();
        List<String> titleY = new ArrayList<String>();
        float average = (int) ((maxValue - minValue) / this.getLatitudeNum()) / 100 * 100;
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
        String value = String.valueOf((int) Math
                .floor(((int) maxValue) / 100 * 100));
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
        if (stickData.size() == 0) {
            return;
        }

        Paint mPaintStick = new Paint();
        mPaintStick.setColor(stickFillColor);

        float stickWidth = getDataQuadrantPaddingWidth() / displayNumber
                - stickSpacing;
        float stickX = getDataQuadrantPaddingStartX();

        for (int i = displayFrom;
             i < displayFrom + displayNumber;
             i++) {
            IMeasurable stick = stickData.get(i);
            float highY = (float) ((1f - (stick.getHigh() - minValue)
                    / (maxValue - minValue))
                    * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());
            float lowY = (float) ((1f - (stick.getLow() - minValue)
                    / (maxValue - minValue))
                    * (getDataQuadrantPaddingHeight()) + getDataQuadrantPaddingStartY());

            // stick or line?
            if (stickWidth >= 2f) {
                canvas.drawRect(stickX, highY, stickX + stickWidth, lowY,
                        mPaintStick);
            } else {
                canvas.drawLine(stickX, highY, stickX, lowY, mPaintStick);
            }

            // next x
            stickX = stickX + stickSpacing + stickWidth;
        }
    }

    protected final int NONE = 0;
    protected final int ZOOM = 1;
    protected final int DOWN = 2;

    protected float olddistance = 0f;
    protected float newdistance = 0f;

    protected int touchMode;

    protected PointF startPoint;
    protected PointF startPointA;
    protected PointF startPointB;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null == stickData || stickData.size() == 0) {
            return false;
        }

        final float MIN_LENGTH = (super.getWidth() / 40) < 5 ? 5 : (super
                .getWidth() / 50);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchMode = DOWN;
                if (event.getPointerCount() == 1) {
                    startPoint = new PointF(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                touchMode = NONE;
                startPointA = null;
                startPointB = null;
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_POINTER_UP:
                touchMode = NONE;
                startPointA = null;
                startPointB = null;
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                olddistance = calcDistance(event);
                if (olddistance > MIN_LENGTH) {
                    touchMode = ZOOM;
                    startPointA = new PointF(event.getX(0), event.getY(0));
                    startPointB = new PointF(event.getX(1), event.getY(1));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchMode == ZOOM) {
                    newdistance = calcDistance(event);
                    if (newdistance > MIN_LENGTH) {
                        if (startPointA.x >= event.getX(0)
                                && startPointB.x >= event.getX(1)) {
                            if (displayFrom + displayNumber + 2 < stickData.size()) {
                                displayFrom = displayFrom + 2;
                            }
                        } else if (startPointA.x <= event.getX(0)
                                && startPointB.x <= event.getX(1)) {
                            if (displayFrom > 2) {
                                displayFrom = displayFrom - 2;
                            }
                        } else {
                            if (Math.abs(newdistance - olddistance) > MIN_LENGTH) {
                                if (newdistance > olddistance) {
                                    zoomIn();
                                } else {
                                    zoomOut();
                                }

                                olddistance = newdistance;
                            }
                        }
                        startPointA = new PointF(event.getX(0), event.getY(0));
                        startPointB = new PointF(event.getX(1), event.getY(1));

                        super.postInvalidate();

                    }
                } else {

                    if (event.getPointerCount() == 1) {
                        float moveXdistance = Math.abs(event.getX() - startPoint.x);
                        float moveYdistance = Math.abs(event.getY() - startPoint.y);

                        if (moveXdistance > 1 || moveYdistance > 1) {

                            super.onTouchEvent(event);

                            startPoint = new PointF(event.getX(), event.getY());
                        }
                    }
                }
                break;
        }
        return true;
    }

    protected float calcDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    protected void zoomIn() {
        if (displayNumber > minDisplayNumber) {
            if (zoomBaseLine == ZOOM_BASE_LINE_CENTER) {
                displayNumber = displayNumber - 2;
                displayFrom = displayFrom + 1;
            } else if (zoomBaseLine == ZOOM_BASE_LINE_LEFT) {
                displayNumber = displayNumber - 2;
            } else if (zoomBaseLine == ZOOM_BASE_LINE_RIGHT) {
                displayNumber = displayNumber - 2;
                displayFrom = displayFrom + 2;
            }
            if (displayNumber < minDisplayNumber) {
                displayNumber = minDisplayNumber;
            }
            if (displayFrom + displayNumber >= stickData.size()) {
                displayFrom = stickData.size() - displayNumber;
            }
        }
    }

    protected void zoomOut() {
        if (displayNumber < stickData.size() - 1) {
            if (displayNumber + 2 > stickData.size() - 1) {
                displayNumber = stickData.size() - 1;
                displayFrom = 0;
            } else {

                if (zoomBaseLine == ZOOM_BASE_LINE_CENTER) {
                    displayNumber = displayNumber + 2;
                    if (displayFrom > 1) {
                        displayFrom = displayFrom - 1;
                    } else {
                        displayFrom = 0;
                    }
                } else if (zoomBaseLine == ZOOM_BASE_LINE_LEFT) {
                    displayNumber = displayNumber + 2;
                } else if (zoomBaseLine == ZOOM_BASE_LINE_RIGHT) {
                    displayNumber = displayNumber + 2;
                    if (displayFrom > 2) {
                        displayFrom = displayFrom - 2;
                    } else {
                        displayFrom = 0;
                    }
                }
            }

            if (displayFrom + displayNumber >= stickData.size()) {
                displayNumber = stickData.size() - displayFrom;
            }
        }
    }

    public void pushData(StickEntity entity) {
        if (null != entity) {
            addData(entity);
            super.postInvalidate();
        }
    }

    public void addData(IStickEntity entity) {
        if (null != entity) {
            // add
            stickData.add(entity);
            if (this.maxValue < entity.getHigh()) {
                this.maxValue = 100 + ((int) entity.getHigh()) / 100 * 100;
            }

        }
    }

    public int getDisplayFrom() {
        return displayFrom;
    }

    public void setDisplayFrom(int displayFrom) {
        this.displayFrom = displayFrom;
    }

    public int getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(int displayNumber) {
        this.displayNumber = displayNumber;
    }

    public int getMinDisplayNumber() {
        return minDisplayNumber;
    }

    public void setMinDisplayNumber(int minDisplayNumber) {
        this.minDisplayNumber = minDisplayNumber;
    }

    public int getZoomBaseLine() {
        return zoomBaseLine;
    }

    public void setZoomBaseLine(int zoomBaseLine) {
        this.zoomBaseLine = zoomBaseLine;
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

    public int getStickSpacing() {
        return stickSpacing;
    }

    public void setStickSpacing(int stickSpacing) {
        this.stickSpacing = stickSpacing;
    }

}
