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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.PieChartView;
import net.bither.ui.base.RotatableFrameLayout;
import net.bither.util.MarketUtil;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtilWrapper;

import java.math.BigInteger;

public class DialogTotalBtc extends DialogWithArrow implements PieChartView.RotateListener {
    private static final float PieChartMarginRate = 0.05f;
    private static final float LogoSizeRate = 200.0f / 640.0f;

    private TextView tvBtc;
    private PieChartView vPieChart;
    private FrameLayout flPieContainer;
    private ImageView ivPrivate;
    private TextView tvPrivate;
    private TextView tvPrivateMoney;
    private ImageView ivPrivateSymbol;
    private ImageView ivWatchOnly;
    private TextView tvWatchOnly;
    private TextView tvWatchOnlyMoney;
    private ImageView ivWatchOnlySymbol;
    private ImageView ivHDM;
    private TextView tvHDM;
    private TextView tvHDMMoney;
    private ImageView ivHDMSymbol;
    private ImageView ivHD;
    private TextView tvHD;
    private TextView tvHDMoney;
    private ImageView ivHDSymbol;
    private ImageView ivHdMonitored;
    private TextView tvHdMonitored;
    private TextView tvHdMonitoredMoney;
    private ImageView ivHdMonitoredSymbol;
    private ImageView ivHdmEnterprise;
    private TextView tvHdmEnterprise;
    private TextView tvHdmEnterpriseMoney;
    private ImageView ivHdmEnterpriseSymbol;
    private LinearLayout llPrivate;
    private LinearLayout llWatchOnly;
    private LinearLayout llHDM;
    private LinearLayout llHD;
    private LinearLayout llHdMonitored;
    private LinearLayout llHdmEnterprise;
    private RotatableFrameLayout flLogo;

    private BigInteger btcPrivate;
    private BigInteger btcWatchOnly;
    private BigInteger btcHdm;
    private BigInteger btcHd;
    private BigInteger btcHdMonitored;
    private BigInteger btcEnterpriseHdm;

    private double price = 0;

    public DialogTotalBtc(Context context) {
        super(context);
        setContentView(R.layout.dialog_total_btc);
        tvBtc = (TextView) findViewById(R.id.tv_btc);
        vPieChart = (PieChartView) findViewById(R.id.pie);
        flPieContainer = (FrameLayout) findViewById(R.id.fl_pie_container);
        ivPrivate = (ImageView) findViewById(R.id.iv_private);
        tvPrivate = (TextView) findViewById(R.id.tv_private);
        tvPrivateMoney = (TextView) findViewById(R.id.tv_private_money);
        ivPrivateSymbol = (ImageView) findViewById(R.id.iv_private_symbol);
        ivWatchOnly = (ImageView) findViewById(R.id.iv_watchonly);
        tvWatchOnly = (TextView) findViewById(R.id.tv_watchonly);
        tvWatchOnlyMoney = (TextView) findViewById(R.id.tv_watchonly_money);
        ivWatchOnlySymbol = (ImageView) findViewById(R.id.iv_watchonly_symbol);
        ivHDM = (ImageView) findViewById(R.id.iv_hdm);
        tvHDM = (TextView) findViewById(R.id.tv_hdm);
        tvHDMMoney = (TextView) findViewById(R.id.tv_hdm_money);
        ivHDMSymbol = (ImageView) findViewById(R.id.iv_hdm_symbol);
        ivHD = (ImageView) findViewById(R.id.iv_hd);
        tvHD = (TextView) findViewById(R.id.tv_hd);
        tvHDMoney = (TextView) findViewById(R.id.tv_hd_money);
        ivHDSymbol = (ImageView) findViewById(R.id.iv_hd_symbol);
        ivHdMonitored = (ImageView) findViewById(R.id.iv_hd_monitored);
        tvHdMonitored = (TextView) findViewById(R.id.tv_hd_monitored);
        tvHdMonitoredMoney = (TextView) findViewById(R.id.tv_hd_monitored_money);
        ivHdMonitoredSymbol = (ImageView) findViewById(R.id.iv_hd_monitored_symbol);
        ivHdmEnterprise = (ImageView) findViewById(R.id.iv_hdm_enterprise);
        tvHdmEnterprise = (TextView) findViewById(R.id.tv_hdm_enterprise);
        tvHdmEnterpriseMoney = (TextView) findViewById(R.id.tv_hdm_enterprise_money);
        ivHdmEnterpriseSymbol = (ImageView) findViewById(R.id.iv_hdm_enterprise_symbol);
        llPrivate = (LinearLayout) findViewById(R.id.ll_private);
        llWatchOnly = (LinearLayout) findViewById(R.id.ll_watchonly);
        llHDM = (LinearLayout) findViewById(R.id.ll_hdm);
        llHD = (LinearLayout) findViewById(R.id.ll_hd);
        llHdMonitored = (LinearLayout) findViewById(R.id.ll_hd_monitored);
        llHdmEnterprise = (LinearLayout) findViewById(R.id.ll_hdm_enterprise);
        flLogo = (RotatableFrameLayout) findViewById(R.id.fl_logo);
        findViewById(R.id.ll_below_chart).setOnClickListener(dismissClick);
        ivHD.setBackgroundDrawable(vPieChart.getSymbolForIndex(0));
        ivHdMonitored.setBackgroundDrawable(vPieChart.getSymbolForIndex(1));
        ivHDM.setBackgroundDrawable(vPieChart.getSymbolForIndex(2));
        ivPrivate.setBackgroundDrawable(vPieChart.getSymbolForIndex(3));
        ivWatchOnly.setBackgroundDrawable(vPieChart.getSymbolForIndex(4));
        ivHdmEnterprise.setBackgroundDrawable(vPieChart.getSymbolForIndex(5));
        vPieChart.setRotateListener(this);
    }

    public void setPrivateWatchOnlyHDMAndHD(BigInteger btcPrivate, BigInteger btcWatchOnly,
                                            BigInteger btcHdm, BigInteger btcHd, BigInteger
                                                    btcHdMonitored, BigInteger btcEnterpriseHdm) {

        BigInteger total = BigInteger.ZERO;
        this.btcPrivate = btcPrivate;
        this.btcWatchOnly = btcWatchOnly;
        this.btcHdm = btcHdm;
        this.btcHd = btcHd;
        this.btcHdMonitored = btcHdMonitored;
        this.btcEnterpriseHdm = btcEnterpriseHdm;
        int count = 0;
        if (btcPrivate != null && btcPrivate.signum() > 0) {
            total = total.add(btcPrivate);
            count++;
        }
        if (btcWatchOnly != null && btcWatchOnly.signum() > 0) {
            total = total.add(btcWatchOnly);
            count++;
        }
        if (btcHdm != null && btcHdm.signum() > 0) {
            total = total.add(btcHdm);
            count++;
        }
        if (btcHd != null && btcHd.signum() > 0) {
            total = total.add(btcHd);
            count++;
        }
        if (btcHdMonitored != null && btcHdMonitored.signum() > 0) {
            total = total.add(btcHdMonitored);
            count++;
        }
        if (btcEnterpriseHdm != null && btcEnterpriseHdm.signum() > 0) {
            total = total.add(btcEnterpriseHdm);
            count++;
        }
        tvBtc.setText(UnitUtilWrapper.formatValue(total.longValue()));
        Bitmap btcSymbol = UnitUtilWrapper.getBtcSlimSymbol(tvPrivate);
        ivPrivateSymbol.setImageBitmap(btcSymbol);
        ivWatchOnlySymbol.setImageBitmap(btcSymbol);
        ivHDMSymbol.setImageBitmap(btcSymbol);
        ivHDSymbol.setImageBitmap(btcSymbol);
        ivHdMonitoredSymbol.setImageBitmap(btcSymbol);
        ivHdmEnterpriseSymbol.setImageBitmap(btcSymbol);
        if (btcPrivate != null && btcPrivate.signum() > 0) {
            tvPrivate.setText(UnitUtilWrapper.formatValue(btcPrivate.longValue()));
            llPrivate.setVisibility(View.VISIBLE);
        } else {
            llPrivate.setVisibility(View.GONE);
        }
        if (btcWatchOnly != null && btcWatchOnly.signum() > 0) {
            tvWatchOnly.setText(UnitUtilWrapper.formatValue(btcWatchOnly.longValue()));
            llWatchOnly.setVisibility(View.VISIBLE);
        } else {
            llWatchOnly.setVisibility(View.GONE);
        }
        if (btcHdm != null && btcHdm.signum() > 0) {
            tvHDM.setText(UnitUtilWrapper.formatValue(btcHdm.longValue()));
            llHDM.setVisibility(View.VISIBLE);
        } else {
            llHDM.setVisibility(View.GONE);
        }
        if (btcHd != null && btcHd.signum() > 0) {
            tvHD.setText(UnitUtilWrapper.formatValue(btcHd.longValue()));
            llHD.setVisibility(View.VISIBLE);
        } else {
            llHD.setVisibility(View.GONE);
        }
        if (btcHdMonitored != null && btcHdMonitored.signum() > 0) {
            tvHdMonitored.setText(UnitUtilWrapper.formatValue(btcHdMonitored.longValue()));
            llHdMonitored.setVisibility(View.VISIBLE);
        } else {
            llHdMonitored.setVisibility(View.GONE);
        }
        if (btcEnterpriseHdm != null && btcEnterpriseHdm.signum() > 0) {
            tvHdmEnterprise.setText(UnitUtilWrapper.formatValue(btcEnterpriseHdm.longValue()));
            llHdmEnterprise.setVisibility(View.VISIBLE);
        } else {
            llHdmEnterprise.setVisibility(View.GONE);
        }

        int minusSize = UIUtil.dip2pix(80);
        if (count > 4) {
            minusSize = UIUtil.dip2pix(100);
        }
        flPieContainer.getLayoutParams().height = flPieContainer.getLayoutParams().width = UIUtil
                .getScreenWidth() - minusSize;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vPieChart.getLayoutParams();
        int margin = (int) (flPieContainer.getLayoutParams().width * PieChartMarginRate);
        lp.topMargin = margin;
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        lp.bottomMargin = margin;
        flLogo.getLayoutParams().width = flLogo.getLayoutParams().height = (int) (flPieContainer
                .getLayoutParams().width * LogoSizeRate);
    }

    @Override
    public void show() {
        vPieChart.setStartAngle(PieChartView.DefaultStartAngle);
        vPieChart.setTotalAngle(0);
        Ticker ticker = MarketUtil.getTickerOfDefaultMarket();
        if (ticker != null) {
            price = ticker.getDefaultExchangePrice();
        } else {
            price = 0;
        }
        String currencySymbol = AppSharedPreference.getInstance().getDefaultExchangeType()
                .getSymbol();
        if (btcPrivate != null && btcPrivate.signum() > 0 && price > 0) {
            tvPrivateMoney.setVisibility(View.VISIBLE);
            tvPrivateMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString(
                    (double) btcPrivate.longValue() / 100000000.0 * price));
        } else {
            tvPrivateMoney.setVisibility(View.GONE);
        }
        if (btcWatchOnly != null && btcWatchOnly.signum() > 0 && price > 0) {
            tvWatchOnlyMoney.setVisibility(View.VISIBLE);
            tvWatchOnlyMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString(
                    (double) btcWatchOnly.longValue() / 100000000.0 * price));
        } else {
            tvWatchOnlyMoney.setVisibility(View.GONE);
        }
        if (btcHdm != null && btcHdm.signum() > 0 && price > 0) {
            tvHDMMoney.setVisibility(View.VISIBLE);
            tvHDMMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString((double)
                    btcHdm.longValue() / 100000000.0 * price));
        } else {
            tvHDMMoney.setVisibility(View.GONE);
        }
        if (btcHd != null && btcHd.signum() > 0 && price > 0) {
            tvHDMoney.setVisibility(View.VISIBLE);
            tvHDMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString((double)
                    btcHd.longValue() / 100000000.0 * price));
        } else {
            tvHDMoney.setVisibility(View.GONE);
        }
        if (btcHdMonitored != null && btcHdMonitored.signum() > 0 && price > 0) {
            tvHdMonitoredMoney.setVisibility(View.VISIBLE);
            tvHdMonitoredMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString(
                    (double) btcHdMonitored.longValue() / 100000000.0 * price));
        } else {
            tvHdMonitoredMoney.setVisibility(View.GONE);
        }
        if (btcEnterpriseHdm != null && btcEnterpriseHdm.signum() > 0 && price > 0) {
            tvHdmEnterpriseMoney.setVisibility(View.VISIBLE);
            tvHdmEnterpriseMoney.setText(currencySymbol + " " + Utils.formatDoubleToMoneyString(
                    (double) btcEnterpriseHdm.longValue() / 100000000.0 * price));
        } else {
            tvHdmEnterpriseMoney.setVisibility(View.GONE);
        }
        super.show();
        vPieChart.postDelayed(new Runnable() {
            @Override
            public void run() {
                vPieChart.setAmounts(btcHd == null ? BigInteger.ZERO : btcHd, btcHdMonitored ==
                        null ? BigInteger.ZERO : btcHdMonitored, btcHdm == null ? BigInteger.ZERO
                        : btcHdm, btcPrivate == null ? BigInteger.ZERO : btcPrivate, btcWatchOnly
                        == null ? BigInteger.ZERO : btcWatchOnly,
                        btcEnterpriseHdm == null ? BigInteger.ZERO : btcEnterpriseHdm);
            }
        }, 100);
    }

    private View.OnClickListener dismissClick = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            dismiss();
        }
    };

    @Override
    public void onRotationChanged(float rotation) {
        flLogo.setRotation(rotation);
    }
}
