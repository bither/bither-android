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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.FrameLayout;


/**
 * Created by songchenwen on 14-6-10.
 */

@SuppressLint("NewApi")
public class RotatableFrameLayout extends FrameLayout {
    private float rotation;

    public RotatableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public RotatableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotatableFrameLayout(Context context) {
        super(context);
    }

    @Override
    public void setRotation(float rotation) {
        this.rotation = rotation;
        if (needWrap()) {
            causeDraw();
        } else {
            super.setRotation(rotation);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needWrap()) {
            canvas.save();
            canvas.rotate(rotation, getWidth() / 2, getHeight() / 2);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }

    @Override
    public float getRotation() {
        if (needWrap()) {
            return rotation;
        } else {
            return super.getRotation();
        }
    }

    public void causeDraw() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private boolean needWrap() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    }

}
