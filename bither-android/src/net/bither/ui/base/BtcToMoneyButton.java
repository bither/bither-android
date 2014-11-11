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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtil;

public class BtcToMoneyButton extends Button implements OnClickListener,
        MarketTickerChangedObserver {
    private double price = 0;
    private long btc;
    private boolean showMoney = false;

    public BtcToMoneyButton(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        setOnClickListener(this);
        setCompoundDrawablePadding(UIUtil.dip2pix(3));
    }

    public BtcToMoneyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BtcToMoneyButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setAmount(long btc) {
        this.btc = btc;
        this.showMoney = false;
        if (btc >= 0) {
            setBackgroundResource(R.drawable.btn_small_green_selector);
        } else {
            setBackgroundResource(R.drawable.btn_small_red_selector);
        }
        setText(UnitUtil.formatValue(btc));
        setCompoundDrawables(getSymbolDrawable(), null, null, null);
    }

    private Drawable getSymbolDrawable() {
        if (showMoney) {
            return null;
        } else {
            Bitmap bmp = UnitUtil.getBtcSlimSymbol(this);
            BitmapDrawable d = new BitmapDrawable(getResources(), bmp);
            d.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
            return d;
        }
    }

    @Override
    public void onClick(View v) {
        if (showMoney) {
            setText(UnitUtil.formatValue(btc));
            showMoney = false;
        } else {
            getPrice();
            if (price != 0) {
                double money = (double) btc / 100000000.0 * price;
                setText(AppSharedPreference.getInstance().getDefaultExchangeType().getSymbol() +
                        Utils.formatDoubleToMoneyString(money));
                showMoney = true;
            }

        }
        setCompoundDrawables(getSymbolDrawable(), null, null, null);
    }

    private void getPrice() {
        Ticker ticker = MarketUtil.getTickerOfDefaultMarket();
        if (ticker != null) {
            price = ticker.getDefaultExchangePrice();
        } else {
            price = 0;
        }
    }

    public void onPause() {

    }

    public void onResume() {
        if (btc != 0) {
            getPrice();
            showBtcInfo();
        }
    }

    private void showBtcInfo() {
        if (!showMoney) {
            setText(UnitUtil.formatValue(btc));
        } else {
            getPrice();
            if (price != 0) {
                double money = (double) btc / 100000000.0 * price;
                setText(AppSharedPreference.getInstance().getDefaultExchangeType().getSymbol() +
                        Utils.formatDoubleToMoneyString(money));
            }

        }
        setCompoundDrawables(getSymbolDrawable(), null, null, null);
    }

    @Override
    public void onMarketTickerChanged() {
        getPrice();
        showBtcInfo();
    }
}
