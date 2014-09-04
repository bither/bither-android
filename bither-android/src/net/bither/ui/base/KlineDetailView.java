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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.AnimationUtil;

public class KlineDetailView extends LinearLayout {
    private TextView tvTime;
    private TextView tvOpen;
    private TextView tvHigh;
    private TextView tvLow;
    private TextView tvClose;
    private TextView tvTenLine;
    private TextView tvThirtyLine;
    private TextView tvVolume;
    private boolean isFirst = true;

    public KlineDetailView(Context context) {
        super(context);
        initView();
    }

    public KlineDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();

    }

    public KlineDetailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        isFirst = true;
        removeAllViews();
        addView(LayoutInflater.from(getContext()).inflate(
                        R.layout.kline_detail_view, null), LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        this.tvTime = (TextView) findViewById(R.id.tv_kline_time);
        this.tvOpen = (TextView) findViewById(R.id.tv_price_open);
        this.tvHigh = (TextView) findViewById(R.id.tv_price_high);
        this.tvLow = (TextView) findViewById(R.id.tv_price_low);
        this.tvClose = (TextView) findViewById(R.id.tv_price_close);
        this.tvTenLine = (TextView) findViewById(R.id.tv_ten_line);
        this.tvThirtyLine = (TextView) findViewById(R.id.tv_thirty_line);
        this.tvVolume = (TextView) findViewById(R.id.tv_volume);
        //TODO Used to calculate the widht, set the default value
        this.tvVolume.setText("10000.00");

    }

    public void setContent(String time, String open, String high, String low,
                           String close, String tenLine, String thirtyLine, String volume) {

        this.tvTime.setText(time);
        this.tvOpen.setText(open);
        this.tvHigh.setText(high);
        this.tvLow.setText(low);
        this.tvClose.setText(close);
        this.tvTenLine.setText(tenLine);
        this.tvThirtyLine.setText(thirtyLine);
        this.tvVolume.setText(volume);

    }

    public void hide() {
        AnimationUtil.fadeIn(KlineDetailView.this);
    }


    public void notifyViewMove(int x, int y, int parentWidth, int parentHight) {
        clearAnimation();

        moveView(x, y, parentWidth, parentHight);

    }

    private void moveView(int x, int y, int parentWidth, int parentHight) {
        int leftMargin = 0;
        int bottomMargin = 0;
        if (x > parentWidth / 2) {
            leftMargin = x - getWidth();
        } else {
            leftMargin = x;
        }
        if (getHeight() == 0) {
            bottomMargin = 10;
        } else {
            if (y + getHeight() > parentHight) {
                bottomMargin = parentHight - getHeight() - 10;
            } else {
                bottomMargin = y;
            }
        }
        if (isFirst) {
            isFirst = false;
            display(leftMargin, bottomMargin);
        } else {
            AnimationUtil.moveMarginAnimation(KlineDetailView.this, leftMargin,
                    bottomMargin);
        }
    }

    private void display(int leftMargin, int bottomMargin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.leftMargin = leftMargin;
        params.bottomMargin = bottomMargin;
        setLayoutParams(params);
        AnimationUtil.fadeOut(KlineDetailView.this);
    }
}
