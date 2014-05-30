/*
 * SlipAreaChart.java
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

import java.util.List;

import net.bither.charts.entity.DateValueEntity;
import net.bither.charts.entity.LineEntity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

public class SlipAreaChart extends SlipLineChart {

    public SlipAreaChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SlipAreaChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlipAreaChart(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw lines
        drawAreas(canvas);
    }

    protected void drawAreas(Canvas canvas) {
        if (null == linesData) {
            return;
        }
        // distance between two points
        float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
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
            mPaint.setAlpha(70);
            mPaint.setAntiAlias(true);

            // set start point’s X
            startX = getDataQuadrantPaddingStartX() + lineLength / 2f;
            Path linePath = new Path();
            for (int j = displayFrom;
                 j < displayFrom + displayNumber;
                 j++) {
                float value = lineData.get(j).getValue();
                // calculate Y
                float valueY = (float) ((1f - (value - minValue)
                        / (maxValue - minValue)) * getDataQuadrantPaddingHeight())
                        + getDataQuadrantPaddingStartY();

                // if is not last point connect to previous point
                if (j == displayFrom) {
                    linePath.moveTo(startX, getDataQuadrantPaddingEndY());
                    linePath.lineTo(startX, valueY);
                } else if (j == displayFrom + displayNumber - 1) {
                    linePath.lineTo(startX, valueY);
                    linePath.lineTo(startX, getDataQuadrantPaddingEndY());
                } else {
                    linePath.lineTo(startX, valueY);
                }
                startX = startX + 1 + lineLength;
            }
            linePath.close();
            canvas.drawPath(linePath, mPaint);
        }
    }
}
