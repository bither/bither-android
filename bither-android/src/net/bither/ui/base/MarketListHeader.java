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
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.MarketDetailActivity;
import net.bither.model.Market;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.util.CurrencySymbolUtil;
import net.bither.util.ExchangeUtil;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;

public class MarketListHeader extends FrameLayout implements
        MarketTickerChangedObserver {
    private static final int LightScanInterval = 1200;
    public static int BgAnimDuration = 600;
    private View ivLight;
    private Animation refreshAnim = AnimationUtils.loadAnimation(getContext(),
            R.anim.check_light_scan);

    private TextView tvName;
    private TextView tvSymbol;
    private TextView tvPrice;
    private TextView tvHigh;
    private TextView tvLow;
    private TextView tvVolume;
    private TextView tvSell;
    private TextView tvBuy;
    private ImageView ivVolumeSymbol;

    private TrendingGraphicView vTrending;
    private LinearLayout llTrending;

    private Market mMarket;
    private OnClickListener marketDetailClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mMarket != null) {
                Intent intent = new Intent(getContext(),
                        MarketDetailActivity.class);
                intent.putExtra(BitherSetting.INTENT_REF.MARKET_INTENT,
                        mMarket.getMarketType());
                getContext().startActivity(intent);
            }
        }
    };
    private BgHolder bg;
    private OnTouchListener trendingTouch = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            vTrending.causeDraw();
            return false;
        }
    };

    public MarketListHeader(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        removeAllViews();
        addView(LayoutInflater.from(getContext()).inflate(
                        R.layout.layout_market_list_header, null),
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );
        ivLight = findViewById(R.id.iv_light);
        tvName = (TextView) findViewById(R.id.tv_market_name);
        tvSymbol = (TextView) findViewById(R.id.tv_currency_symbol);
        tvPrice = (TextView) findViewById(R.id.tv_new_price);
        tvHigh = (TextView) findViewById(R.id.tv_high);
        tvLow = (TextView) findViewById(R.id.tv_low);
        tvVolume = (TextView) findViewById(R.id.tv_volume);
        tvSell = (TextView) findViewById(R.id.tv_sell);
        tvBuy = (TextView) findViewById(R.id.tv_buy);
        ivVolumeSymbol = (ImageView) findViewById(R.id.iv_volume_symbol);
        vTrending = (TrendingGraphicView) findViewById(R.id.v_trending);
        llTrending = (LinearLayout) findViewById(R.id.ll_trending);
        refreshAnim.setDuration(LightScanInterval);
        refreshAnim.setFillBefore(false);
        refreshAnim.setRepeatCount(0);
        refreshAnim.setFillAfter(false);
        ivVolumeSymbol
                .setImageBitmap(CurrencySymbolUtil.getBtcSymbol(tvVolume));
        llTrending.setOnClickListener(marketDetailClick);
        llTrending.setOnTouchListener(trendingTouch);
        setMarket(MarketUtil.getDefaultMarket());
    }

    private void showMarket() {
        if (mMarket == null) {
            return;
        }
        bg.setEndColor(mMarket.getMarketColor());
        tvName.setText(mMarket.getName());
        String symbol = "";
        if (mMarket.getTicker() != null) {
            if (ExchangeUtil.getExchangeRate() > 0) {
                symbol = AppSharedPreference.getInstance()
                        .getDefaultExchangeType().getSymbol();
            } else {
                symbol = ExchangeUtil.getExchangeType(mMarket.getMarketType())
                        .getSymbol();
            }
        }
        tvSymbol.setText(symbol);
        if (mMarket != null) {
            vTrending.setMarketType(mMarket.getMarketType());
        }
        Ticker ticker = mMarket.getTicker();
        if (ticker == null) {
            tvPrice.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvHigh.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvLow.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvVolume.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvSell.setText(BitherSetting.UNKONW_ADDRESS_STRING);
            tvBuy.setText(BitherSetting.UNKONW_ADDRESS_STRING);
        } else {
            tvPrice.setText(StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangePrice()));
            tvHigh.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeHigh()));
            tvLow.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeLow()));
            tvVolume.setText(StringUtil.formatDoubleToMoneyString(ticker
                    .getAmount()));
            tvSell.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeSell()));
            tvBuy.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeBuy()));
        }
    }

    public MarketListHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MarketListHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public Market getMarket() {
        return mMarket;
    }

    public void setMarket(Market market) {
        if (mMarket == null) {
            bg = new BgHolder(market.getMarketColor());
        }
        mMarket = market;
        showMarket();
    }

    @Override
    public void onMarketTickerChanged() {
        if (mMarket == null) {
            return;
        }
        showMarket();
        if (!refreshAnim.hasStarted() || refreshAnim.hasEnded()) {
            ivLight.startAnimation(refreshAnim);
        }
    }

    public void onPause() {

    }

    public void onResume() {
        showMarket();
    }

    private class BgHolder {
        private float animProgress = 0;
        private ArgbEvaluator evaluator = new ArgbEvaluator();
        private int startColor;
        private int endColor;
        private int currentColor;
        private ObjectAnimator animator;

        public BgHolder(int startColor) {
            this.startColor = startColor;
            this.endColor = startColor;
            setBg((Integer) evaluator.evaluate(0, startColor, startColor));
        }

        private void setBg(int color) {
            currentColor = color;
            MarketListHeader.this.setBackgroundColor(color);
        }

        public float getAnimProgress() {
            return animProgress;
        }

        public void setAnimProgress(float animProgress) {
            this.animProgress = animProgress;
            if (currentColor == endColor) {
                setBg(currentColor);
            } else {
                setBg((Integer) evaluator.evaluate(animProgress, startColor,
                        endColor));
            }
        }

        public void setEndColor(int endColor) {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            startColor = currentColor;
            this.endColor = endColor;
            animProgress = 0;
            animator = ObjectAnimator.ofFloat(this, "animProgress", 1);
            animator.setDuration(BgAnimDuration);
            animator.start();
        }
    }

}
