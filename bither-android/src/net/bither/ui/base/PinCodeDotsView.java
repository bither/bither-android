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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-11-5.
 */
public class PinCodeDotsView extends View {
    private static final float Padding = 1;
    private int dotColor;
    private int filledCount;
    private int totalDotCount = 4;

    private Paint circlePaint;
    private Paint dotPaint;

    public PinCodeDotsView(Context context) {
        super(context);
        init();
    }

    public PinCodeDotsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PinCodeDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint = new Paint();
        circlePaint.setColor(dotColor);
        circlePaint.setStrokeWidth(UIUtil.dip2pix(1.6f));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setAntiAlias(true);
        dotPaint = new Paint(circlePaint);
        dotPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        float size = canvas.getHeight() - circlePaint.getStrokeWidth() * 2;
        float distance = (canvas.getWidth() - circlePaint.getStrokeWidth() * 2 - size * 4) / 3;
        float y = canvas.getHeight() / 2.0f;
        float radius = size / 2.0f;
        for (int i = 0;
             i < totalDotCount;
             i++) {
            float x = i * (size + distance) + size / 2.0f + circlePaint.getStrokeWidth();
            canvas.drawCircle(x, y, radius, i < filledCount ? dotPaint : circlePaint);
        }
        super.draw(canvas);
    }

    public int getDotColor() {
        return dotColor;
    }

    public void setDotColor(int color) {
        dotColor = color;
        circlePaint.setColor(dotColor);
        dotPaint.setColor(dotColor);
        invalidate();
    }

    public int getFilledCount() {
        return filledCount;
    }

    public void setFilledCount(int filledCount) {
        this.filledCount = filledCount;
        invalidate();
    }

    public int getTotalDotCount() {
        return totalDotCount;
    }

    public void setTotalDotCount(int totalDotCount) {
        this.totalDotCount = totalDotCount;
    }
}
