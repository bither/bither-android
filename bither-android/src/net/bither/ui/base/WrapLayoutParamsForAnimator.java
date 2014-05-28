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

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class WrapLayoutParamsForAnimator {
    ViewGroup.LayoutParams lp;
    ViewGroup.MarginLayoutParams mlp;
    View v;

    public WrapLayoutParamsForAnimator(View v) {
        this.v = v;
        this.lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            mlp = (ViewGroup.MarginLayoutParams) lp;
        }
    }

    public int getTopMargin() {
        if (mlp != null) {
            return mlp.topMargin;
        }
        return 0;
    }

    public void setTopMargin(int topMargin) {
        if (mlp != null) {
            mlp.topMargin = topMargin;
            v.requestLayout();
        }
    }

    public int getBottomMargin() {
        if (mlp != null) {
            return mlp.bottomMargin;
        }
        return 0;
    }


    public void setBottomMargin(int bottomMargin) {
        if (mlp != null) {
            mlp.bottomMargin = bottomMargin;
            v.requestLayout();
        }
    }

    public int getLeftMargin() {
        if (mlp != null) {
            return mlp.leftMargin;
        }
        return 0;
    }

    public void setLeftMargin(int leftMargin) {
        if (mlp != null) {
            mlp.leftMargin = leftMargin;
            v.requestLayout();
        }
    }

    public int getRightMargin() {
        if (mlp != null) {
            return mlp.rightMargin;
        }
        return 0;
    }

    public void setRightMargin(int rightMargin) {
        if (mlp != null) {
            mlp.rightMargin = rightMargin;
            v.requestLayout();
        }
    }

    public int getHeight() {
        return lp.height;
    }

    public void setHeight(int height) {
        lp.height = height;
        v.requestLayout();
    }

    public float getLayoutWeight() {
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.lp;
            return lp.weight;
        }
        return 0;
    }

    public void setLayoutWeight(float weight) {
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.lp;
            lp.weight = weight;
            v.requestLayout();
        }
    }

    public int getWidth() {
        return lp.width;
    }

    public void setWidth(int width) {
        lp.width = width;
        v.requestLayout();
    }

}
