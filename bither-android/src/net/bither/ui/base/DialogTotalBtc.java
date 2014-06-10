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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.GenericUtils;
import net.bither.util.UIUtil;

import java.math.BigInteger;

public class DialogTotalBtc extends DialogWithArrow implements PieChartView.RotateListener {
    private static final float PieChartMarginRate = 0.05f;
    private static final float LogoSizeRate = 200.0f / 640.0f;

    private TextView tvBtc;
    private PieChartView vPieChart;
    private FrameLayout flPieContainer;
    private ImageView ivPrivate;
    private TextView tvPrivate;
    private ImageView ivWatchOnly;
    private TextView tvWatchOnly;
    private LinearLayout llPrivate;
    private LinearLayout llWatchOnly;
    private RotatableFrameLayout flLogo;

    private BigInteger btcPrivate;
    private BigInteger btcWatchOnly;

    public DialogTotalBtc(Context context) {
        super(context);
        setContentView(R.layout.dialog_total_btc);
        tvBtc = (TextView) findViewById(R.id.tv_btc);
        vPieChart = (PieChartView) findViewById(R.id.pie);
        flPieContainer = (FrameLayout) findViewById(R.id.fl_pie_container);
        flPieContainer.getLayoutParams().height = flPieContainer.getLayoutParams().width = UIUtil
                .getScreenWidth() - UIUtil.dip2pix(80);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vPieChart.getLayoutParams();
        int margin = (int) (flPieContainer.getLayoutParams().width * PieChartMarginRate);
        lp.topMargin = margin;
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        lp.bottomMargin = margin;
        ivPrivate = (ImageView) findViewById(R.id.iv_private);
        tvPrivate = (TextView) findViewById(R.id.tv_private);
        ivWatchOnly = (ImageView) findViewById(R.id.iv_watchonly);
        tvWatchOnly = (TextView) findViewById(R.id.tv_watchonly);
        llPrivate = (LinearLayout) findViewById(R.id.ll_private);
        llWatchOnly = (LinearLayout) findViewById(R.id.ll_watchonly);
        flLogo = (RotatableFrameLayout) findViewById(R.id.fl_logo);
        ivPrivate.setBackgroundDrawable(vPieChart.getSymbolForIndex(0));
        ivWatchOnly.setBackgroundDrawable(vPieChart.getSymbolForIndex(1));
        flLogo.getLayoutParams().width = flLogo.getLayoutParams().height = (int) (flPieContainer
                .getLayoutParams().width * LogoSizeRate);
        vPieChart.setRotateListener(this);
    }

    public void setPrivateAndWatchOnly(BigInteger btcPrivate, BigInteger btcWatchOnly) {
        BigInteger total = BigInteger.ZERO;
        this.btcPrivate = btcPrivate;
        this.btcWatchOnly = btcWatchOnly;
        if (btcPrivate != null && btcPrivate.signum() > 0) {
            total = total.add(btcPrivate);
        }
        if (btcWatchOnly != null && btcWatchOnly.signum() > 0) {
            total = total.add(btcWatchOnly);
        }
        tvBtc.setText(GenericUtils.formatValue(total));
        if (btcPrivate != null && btcPrivate.signum() > 0) {
            tvPrivate.setText(GenericUtils.formatValue(btcPrivate));
            llPrivate.setVisibility(View.VISIBLE);
        } else {
            llPrivate.setVisibility(View.GONE);
        }
        if (btcWatchOnly != null && btcWatchOnly.signum() > 0) {
            tvWatchOnly.setText(GenericUtils.formatValue(btcWatchOnly));
            llWatchOnly.setVisibility(View.VISIBLE);
        } else {
            llWatchOnly.setVisibility(View.GONE);
        }
    }

    @Override
    public void show() {
        vPieChart.setStartAngle(PieChartView.DefaultStartAngle);
        vPieChart.setTotalAngle(0);
        super.show();
        vPieChart.postDelayed(new Runnable() {
            @Override
            public void run() {
                vPieChart.setAmounts(btcPrivate == null ? BigInteger.ZERO : btcPrivate,
                        btcWatchOnly == null ? BigInteger.ZERO : btcWatchOnly);
            }
        }, 100);
    }

    @Override
    public void onRotationChanged(float rotation) {
        flLogo.setRotation(rotation);
    }
}
