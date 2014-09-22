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

package net.bither.xrandom.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import net.bither.R;
import net.bither.util.UIUtil;

import java.util.Random;

/**
 * Created by songchenwen on 14-9-22.
 */
public class RandomPointView extends View {
    private static final int PointCount = 6;
    private static final int DotsRemainTime = 200;
    private static final int DotsAppearInterval = 80;

    private Random random = new Random();
    private Paint paint = new Paint();
    private boolean hasDots = false;

    public RandomPointView(Context context) {
        super(context);
        firstConfigure();
    }

    public RandomPointView(Context context, AttributeSet attrs) {
        super(context, attrs);
        firstConfigure();
    }

    public RandomPointView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        firstConfigure();
    }

    private void firstConfigure() {
        paint.setColor(getContext().getResources().getColor(R.color.scan_dot));
        paint.setStrokeWidth(UIUtil.dip2pix(3));
        paint.setAlpha(0xa0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        removeCallbacks(redraw);
        canvas.drawARGB(0, 0, 0, 0);
        if (hasDots) {
            hasDots = false;
            postDelayed(redraw, DotsAppearInterval);
        } else {
            hasDots = true;
            int width = getWidth();
            int height = getHeight();
            float[] points = new float[PointCount * 2];
            for (int i = 0;
                 i < PointCount;
                 i++) {
                points[i * 2] = random.nextFloat() * width;
                points[i * 2 + 1] = random.nextFloat() * height;
                canvas.drawPoints(points, paint);
            }
            postDelayed(redraw, DotsRemainTime);
        }
        super.draw(canvas);
    }

    private Runnable redraw = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
}
