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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.util.AnimationUtil;
import net.bither.util.UnitUtilWrapper;

public class MarketDepthDetailView extends LinearLayout {

    private TextView tvOrder;
    private TextView tvPrice;
    private TextView tvVolume;
    private TextView tvSymbol;

    private ImageView ivSymbolBtc;
    private Bitmap btcBit;
    private boolean isFirst = true;

    public MarketDepthDetailView(Context context) {
        super(context);
        initView();
    }

    public MarketDepthDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();

    }

    public MarketDepthDetailView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        isFirst = true;
        removeAllViews();
        addView(LayoutInflater.from(getContext()).inflate(
                        R.layout.market_depth_detail_view, null),
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        );
        this.tvOrder = (TextView) findViewById(R.id.tv_detail_order);
        this.tvPrice = (TextView) findViewById(R.id.tv_detail_price);
        this.tvVolume = (TextView) findViewById(R.id.tv_detail_volume);
        this.ivSymbolBtc = (ImageView) findViewById(R.id.iv_symbol_btc);
        this.btcBit = UnitUtilWrapper.getBtcSlimSymbol(this.tvVolume,
                UnitUtilWrapper.BitcoinUnitWrapper.BTC);
        this.tvSymbol = (TextView) findViewById(R.id.tv_symbol);
        //TODO Used to calculate the widht, set the default value
        this.tvVolume.setText("10000.00");

    }

    public void setContent(String order, String price, String volume) {
        this.tvOrder.setText(order);
        this.tvPrice.setText(price);
        this.tvVolume.setText(volume);
        this.ivSymbolBtc.setImageBitmap(btcBit);
        this.tvSymbol.setText(AppSharedPreference.getInstance()
                .getDefaultExchangeType().getSymbol());
    }

    public void hide() {
        AnimationUtil.fadeIn(MarketDepthDetailView.this);

    }

    public void notifyViewMove(int x, int y, int parentWidth) {
        clearAnimation();
        moveView(x, y, parentWidth);
    }


    private void moveView(int x, int y, int parentWidth) {
        int leftMargin = 0;
        if (x > parentWidth / 2) {
            leftMargin = x - getWidth();
        } else {
            leftMargin = x;
        }
        if (isFirst) {
            isFirst = false;
            display(leftMargin, y);
        } else {
            AnimationUtil.moveMarginAnimation(MarketDepthDetailView.this, leftMargin,
                    y);
        }
    }

    private void display(int leftMargin, int bottomMargin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.leftMargin = leftMargin;
        params.bottomMargin = bottomMargin;
        setLayoutParams(params);
        AnimationUtil.fadeOut(MarketDepthDetailView.this);
    }

}
