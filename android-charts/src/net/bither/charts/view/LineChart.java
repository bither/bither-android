/*
 * LineChart.java
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

import net.bither.charts.entity.DateValueEntity;
import net.bither.charts.entity.LineEntity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;

public class LineChart extends GridChart {

    private List<LineEntity<DateValueEntity>> linesData;
    private int maxPointNum;
    private double minValue;
    private double maxValue;

    public static final boolean DEFAULT_AUTO_CALC_VALUE_RANGE = true;
    private boolean autoCalcValueRange = DEFAULT_AUTO_CALC_VALUE_RANGE;

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void calcDataValueRange() {
        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;
        for (int i = 0;
             i < this.linesData.size();
             i++) {
            LineEntity<DateValueEntity> line = (LineEntity<DateValueEntity>) linesData
                    .get(i);
            if (line == null) {
                continue;
            }
            if (line.isDisplay() == false) {
                continue;
            }
            List<DateValueEntity> lineData = line.getLineData();
            if (lineData == null) {
                continue;
            }

            for (int j = 0;
                 j < lineData.size();
                 j++) {
                DateValueEntity entity;
                if (axisYPosition == AXIS_Y_POSITION_LEFT) {
                    entity = line.getLineData().get(j);
                } else {
                    entity = line.getLineData().get(lineData.size() - 1 - j);
                }

                if (entity.getValue() < minValue) {
                    minValue = entity.getValue();
                }
                if (entity.getValue() > maxValue) {
                    maxValue = entity.getValue();
                }
            }
        }

        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    protected void calcValueRangePaddingZero() {
        double maxValue = this.maxValue;
        double minValue = this.minValue;

        if ((long) maxValue > (long) minValue) {
            if ((maxValue - minValue) < 10. && minValue > 1.) {
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
        int rate = 1;

        if (this.maxValue < 3000) {
            rate = 1;
        } else if (this.maxValue >= 3000 && this.maxValue < 5000) {
            rate = 5;
        } else if (this.maxValue >= 5000 && this.maxValue < 30000) {
            rate = 10;
        } else if (this.maxValue >= 30000 && this.maxValue < 50000) {
            rate = 50;
        } else if (this.maxValue >= 50000 && this.maxValue < 300000) {
            rate = 100;
        } else if (this.maxValue >= 300000 && this.maxValue < 500000) {
            rate = 500;
        } else if (this.maxValue >= 500000 && this.maxValue < 3000000) {
            rate = 1000;
        } else if (this.maxValue >= 3000000 && this.maxValue < 5000000) {
            rate = 5000;
        } else if (this.maxValue >= 5000000 && this.maxValue < 30000000) {
            rate = 10000;
        } else if (this.maxValue >= 30000000 && this.maxValue < 50000000) {
            rate = 50000;
        } else {
            rate = 100000;
        }

        if (this.latitudeNum > 0 && rate > 1
                && (long) (this.minValue) % rate != 0) {
            this.minValue = (long) this.minValue - (long) (this.minValue)
                    % rate;
        }
        if (this.latitudeNum > 0
                && (long) (this.maxValue - this.minValue)
                % (this.latitudeNum * rate) != 0) {
            this.maxValue = (long) this.maxValue + this.latitudeNum * rate
                    - (long) (this.maxValue - this.minValue)
                    % (this.latitudeNum * rate);
        }
    }

    protected void calcValueRange() {
        if (null == this.linesData) {
            this.maxValue = 0;
            this.minValue = 0;
            return;
        }
        if (this.linesData.size() > 0) {
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
        if (autoCalcValueRange) {
            calcValueRange();
        }

        initAxisY();
        initAxisX();

        super.onDraw(canvas);
        drawLines(canvas);

    }

    protected void drawLines(Canvas canvas) {
        if (null == this.linesData) {
            return;
        }
        // distance between two points
        float lineLength = getDataQuadrantPaddingWidth() / maxPointNum - 1;
        // start point‘s X
        float startX;

        // draw lines
        for (int i = 0;
             i < linesData.size();
             i++) {
            LineEntity<DateValueEntity> line = (LineEntity<DateValueEntity>) linesData
                    .get(i);
            if (line == null) {
                continue;
            }
            if (line.isDisplay() == false) {
                continue;
            }
            List<DateValueEntity> lineData = line.getLineData();
            if (lineData == null) {
                continue;
            }

            Paint mPaint = new Paint();
            mPaint.setColor(line.getLineColor());
            mPaint.setAntiAlias(true);
            // start point
            PointF ptFirst = null;
            if (axisYPosition == AXIS_Y_POSITION_LEFT) {
                // set start point’s X
                startX = getDataQuadrantPaddingStartX() + lineLength / 2;
                for (int j = 0;
                     j < lineData.size();
                     j++) {
                    float value = lineData.get(j).getValue();
                    // calculate Y
                    float valueY = (float) ((1f - (value - minValue)
                            / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                            + getDataQuadrantPaddingStartY();

                    // if is not last point connect to previous point
                    if (j > 0) {
                        canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                                mPaint);
                    }
                    // reset
                    ptFirst = new PointF(startX, valueY);
                    startX = startX + 1 + lineLength;
                }
            } else {
                // set start point’s X
                startX = getDataQuadrantPaddingEndX() - lineLength / 2;
                for (int j = lineData.size() - 1;
                     j >= 0;
                     j--) {
                    float value = lineData.get(j).getValue();
                    // calculate Y
                    float valueY = (float) ((1f - (value - minValue)
                            / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                            + getDataQuadrantPaddingStartY();

                    // if is not last point connect to previous point
                    if (j < lineData.size() - 1) {
                        canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                                mPaint);
                    }
                    // reset
                    ptFirst = new PointF(startX, valueY);
                    startX = startX - 1 - lineLength;
                }
            }
        }
    }

    @Override
    public String getAxisXGraduate(Object value) {

        float graduate = Float.valueOf(super.getAxisXGraduate(value));
        int index = (int) Math.floor(graduate * maxPointNum);

        if (index >= maxPointNum) {
            index = maxPointNum - 1;
        } else if (index < 0) {
            index = 0;
        }

        if (null == this.linesData) {
            return "";
        }
        LineEntity<DateValueEntity> line = (LineEntity<DateValueEntity>) linesData
                .get(0);
        if (line == null) {
            return "";
        }
        if (line.isDisplay() == false) {
            return "";
        }
        List<DateValueEntity> lineData = line.getLineData();
        if (lineData == null) {
            return "";
        }
        return String.valueOf(lineData.get(index).getDate());
    }

    @Override
    public String getAxisYGraduate(Object value) {
        float graduate = Float.valueOf(super.getAxisYGraduate(value));
        return String.valueOf((int) Math.floor(graduate * (maxValue - minValue)
                + minValue));
    }

    protected void initAxisY() {
        this.calcValueRange();
        List<String> titleY = new ArrayList<String>();
        float average = (int) ((maxValue - minValue) / this.getLatitudeNum());
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
        String value = String.valueOf((int) Math.floor(((int) maxValue)));
        if (value.length() < super.getLatitudeMaxTitleLength()) {
            while (value.length() < super.getLatitudeMaxTitleLength()) {
                value = " " + value;
            }
        }
        titleY.add(value);

        super.setLatitudeTitles(titleY);
    }

    protected void initAxisX() {
        List<String> titleX = new ArrayList<String>();
        if (null != linesData && linesData.size() > 0) {
            float average = maxPointNum / this.getLongitudeNum();
            for (int i = 0;
                 i < this.getLongitudeNum();
                 i++) {
                int index = (int) Math.floor(i * average);
                if (index > maxPointNum - 1) {
                    index = maxPointNum - 1;
                }
                titleX.add(String.valueOf(
                        linesData.get(0).getLineData().get(index).getDate())
                        .substring(4));
            }
            titleX.add(String.valueOf(
                    linesData.get(0).getLineData().get(maxPointNum - 1)
                            .getDate()
            ).substring(4));
        }
        super.setLongitudeTitles(titleX);
    }

    private final int NONE = 0;
    private final int ZOOM = 1;
    private final int DOWN = 2;

    private float olddistance = 0f;
    private float newdistance = 0f;

    private int touchMode;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final float MIN_LENGTH = super.getWidth() / 40 < 5 ? 5 : super
                .getWidth() / 50;

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
        if (null == linesData || linesData.size() <= 0) {
            return;
        }
        if (maxPointNum > 10) {
            maxPointNum = maxPointNum - 3;
        }
    }

    protected void zoomOut() {
        if (null == linesData || linesData.size() <= 0) {
            return;
        }
        if (maxPointNum < linesData.get(0).getLineData().size() - 1 - 3) {
            maxPointNum = maxPointNum + 3;
        }
    }

    public List<LineEntity<DateValueEntity>> getLinesData() {
        return linesData;
    }

    public void setLinesData(List<LineEntity<DateValueEntity>> linesData) {
        this.linesData = linesData;
    }

    public int getMaxPointNum() {
        return maxPointNum;
    }

    public void setMaxPointNum(int maxPointNum) {
        this.maxPointNum = maxPointNum;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isAutoCalcValueRange() {
        return autoCalcValueRange;
    }

    public void setAutoCalcValueRange(boolean autoCalcValueRange) {
        this.autoCalcValueRange = autoCalcValueRange;
    }

}
