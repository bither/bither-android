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

package net.bither.activity.hot;

import java.util.Date;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.BitherSetting.KlineTimeType;
import net.bither.BitherSetting.MarketType;
import net.bither.R;
import net.bither.charts.event.ITouchEventResponse;
import net.bither.charts.view.MACandleStickChart;
import net.bither.charts.view.MarketDepthChart;
import net.bither.model.Depth;
import net.bither.model.KLine;
import net.bither.model.Market;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.GetExchangeDepthRunnable;
import net.bither.runnable.GetKLineRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.DialogMarketDetailOption;
import net.bither.ui.base.DialogMarketDetailOption.MarketDetailDialogDelegate;
import net.bither.ui.base.DialogProgress;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.KlineDetailView;
import net.bither.ui.base.MarketDepthDetailView;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.ChartsUtil;
import net.bither.util.DateTimeUtil;
import net.bither.util.ExchangeUtil;
import net.bither.util.FileUtil;
import net.bither.util.ImageManageUtil;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class MarketDetailActivity extends SwipeRightActivity implements
        OnCheckedChangeListener, MarketDetailDialogDelegate {
    private final int DISAPPEAR_TIME = 3 * 1000;

    private TextView tvMarketName;
    private RadioGroup rg;
    private MACandleStickChart chartKline;
    private MarketDepthChart chartDepth;
    private ProgressBar pbKline;
    private ProgressBar pbDepth;
    private TextView tvKlineError;
    private TextView tvDepthError;
    private LinearLayout llTicker;
    private TextView tvPrice;
    private TextView tvHigh;
    private TextView tvLow;
    private TextView tvSell;
    private TextView tvBuy;
    private LinearLayout llContent;
    private DialogMarketDetailOption dialogOption;
    private DialogProgress dp;

    private MarketDepthDetailView marketDepthDetailView;

    private KlineDetailView mKLineDetailView;

    private MarketType marketType = null;
    private boolean isKlineRefresh = false;
    private boolean isMarketDepthRefresh = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        Intent intent = getIntent();
        if (intent != null
                && intent.hasExtra(BitherSetting.INTENT_REF.MARKET_INTENT)) {
            marketType = (MarketType) intent
                    .getSerializableExtra(BitherSetting.INTENT_REF.MARKET_INTENT);
        }
        boolean isFromNotif = false;
        if (intent != null && intent.hasExtra(BitherSetting.INTENT_REF.INTENT_FROM_NOTIF)) {
            isFromNotif = intent.getBooleanExtra(BitherSetting.INTENT_REF.INTENT_FROM_NOTIF, false);
        }

        if (marketType == null) {
            finish();
        } else {
            setContentView(R.layout.activity_market_detail);
            initView();
            if (isFromNotif && BitherApplication.hotActivity != null) {
                chartKline.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (BitherApplication.hotActivity != null) {
                            BitherApplication.hotActivity.notifPriceAlert(marketType);
                        }
                    }
                }, 500);

            }
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_back)
                .setOnClickListener(new BackClickListener());
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        tvMarketName = (TextView) findViewById(R.id.tv_market_name);
        pbKline = (ProgressBar) findViewById(R.id.pb_kline);
        pbDepth = (ProgressBar) findViewById(R.id.pb_depth);
        tvKlineError = (TextView) findViewById(R.id.tv_kline_error);
        tvDepthError = (TextView) findViewById(R.id.tv_depth_error);
        tvPrice = (TextView) findViewById(R.id.tv_price);
        tvHigh = (TextView) findViewById(R.id.tv_high);
        tvLow = (TextView) findViewById(R.id.tv_low);
        tvSell = (TextView) findViewById(R.id.tv_sell);
        tvBuy = (TextView) findViewById(R.id.tv_buy);
        llTicker = (LinearLayout) findViewById(R.id.ll_ticker);
        llContent = (LinearLayout) findViewById(R.id.ll_content);
        chartKline = (MACandleStickChart) findViewById(R.id.macandlestickchart);
        chartDepth = (MarketDepthChart) findViewById(R.id.marketdepthchart);
        chartDepth.setTouchEventResponse(marketDepthViewTouchResponse);
        chartKline.setTouchEventResponse(kLineTouchResponse);
        marketDepthDetailView = (MarketDepthDetailView) findViewById(R.id.market_depth_detail);
        mKLineDetailView = (KlineDetailView) findViewById(R.id.klinedetailview);

        rg = (RadioGroup) findViewById(R.id.rg);
        rg.setOnCheckedChangeListener(this);
        onCheckedChanged(rg, rg.getCheckedRadioButtonId());
        dialogOption = new DialogMarketDetailOption(this, this);
        dp = new DialogProgress(this, R.string.please_wait);
        loadDepthData();
        showMarket();
    }

    private void showMarket() {
        Market market = MarketUtil.getMarket(marketType);
        tvMarketName.setText(market.getName());
        Ticker ticker = market.getTicker();
        if (ticker != null) {
            llTicker.setVisibility(View.VISIBLE);
            String symbol;
            if (ExchangeUtil.getExchangeRate() > 0) {
                symbol = AppSharedPreference.getInstance()
                        .getDefaultExchangeType().getSymbol();
            } else {
                symbol = ExchangeUtil.getExchangeType(marketType).getSymbol();
            }
            tvPrice.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangePrice()));
            tvHigh.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeHigh()));
            tvLow.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeLow()));
            tvSell.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeSell()));
            tvBuy.setText(symbol
                    + StringUtil.formatDoubleToMoneyString(ticker
                    .getDefaultExchangeBuy()));
        } else {
            llTicker.setVisibility(View.GONE);
        }
    }

    private void loadKlineData(KlineTimeType klineTimeType) {
        chartKline.clearTounch();
        pbKline.setVisibility(View.VISIBLE);
        chartKline.setVisibility(View.INVISIBLE);
        tvKlineError.setVisibility(View.GONE);
        setKlineRadioButtonEnabled(false);
        GetKLineRunnable getKLineRunnable = new GetKLineRunnable(marketType,
                klineTimeType);
        getKLineRunnable.setHandler(loadKlineDataHandler);
        new Thread(getKLineRunnable).start();
    }

    private void loadDepthData() {
        pbDepth.setVisibility(View.VISIBLE);
        tvDepthError.setVisibility(View.GONE);
        chartDepth.setVisibility(View.INVISIBLE);
        GetExchangeDepthRunnable getExchangeDepthRunnable = new GetExchangeDepthRunnable(
                marketType);
        getExchangeDepthRunnable.setHandler(loadDepthHandler);
        new Thread(getExchangeDepthRunnable).start();

    }

    private Handler loadDepthHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_PREPARE:

                    break;
                case HandlerMessage.MSG_SUCCESS_FROM_CACHE:
                    if (msg.obj != null) {
                        Depth depth = (Depth) msg.obj;
                        ChartsUtil.initMarketDepth(chartDepth, depth,
                                isMarketDepthRefresh);
                        isMarketDepthRefresh = true;
                        pbDepth.setVisibility(View.GONE);
                        tvDepthError.setVisibility(View.GONE);
                        chartDepth.setVisibility(View.VISIBLE);
                    }
                    break;
                case HandlerMessage.MSG_SUCCESS:
                    Depth depth = (Depth) msg.obj;
                    ChartsUtil.initMarketDepth(chartDepth, depth,
                            isMarketDepthRefresh);
                    isMarketDepthRefresh = true;
                    pbDepth.setVisibility(View.GONE);
                    tvDepthError.setVisibility(View.GONE);
                    chartDepth.setVisibility(View.VISIBLE);
                    break;
                case HandlerMessage.MSG_FAILURE:
                    pbDepth.setVisibility(View.GONE);
                    tvDepthError.setVisibility(View.VISIBLE);
                    chartDepth.setVisibility(View.INVISIBLE);
                    break;
                default:
                    break;
            }
        }

        ;
    };

    private Handler loadKlineDataHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_PREPARE:
                    break;
                case HandlerMessage.MSG_SUCCESS_FROM_CACHE:
                    if (msg.obj != null) {
                        KLine kLine = (KLine) msg.obj;
                        ChartsUtil.initMACandleStickChart(chartKline,
                                kLine.getStickEntities(), isKlineRefresh);
                        if (!isKlineRefresh) {
                            isKlineRefresh = true;
                        }
                        tvKlineError.setVisibility(View.GONE);
                        pbKline.setVisibility(View.GONE);
                        chartKline.setVisibility(View.VISIBLE);
                        setKlineRadioButtonEnabled(true);
                    }
                    break;
                case HandlerMessage.MSG_SUCCESS:
                    KLine kLine = (KLine) msg.obj;
                    ChartsUtil.initMACandleStickChart(chartKline,
                            kLine.getStickEntities(), isKlineRefresh);
                    if (!isKlineRefresh) {
                        isKlineRefresh = true;
                    }
                    pbKline.setVisibility(View.GONE);
                    tvKlineError.setVisibility(View.GONE);
                    chartKline.setVisibility(View.VISIBLE);
                    setKlineRadioButtonEnabled(true);
                    break;
                case HandlerMessage.MSG_FAILURE:
                    pbKline.setVisibility(View.GONE);
                    tvKlineError.setVisibility(View.VISIBLE);
                    chartKline.setVisibility(View.INVISIBLE);
                    setKlineRadioButtonEnabled(true);
                    break;
                default:
                    break;
            }
        }

        ;
    };

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_one_minute:
                loadKlineData(KlineTimeType.ONE_MINUTE);
                break;
            case R.id.rb_five_minute:
                loadKlineData(KlineTimeType.FIVE_MINUTES);
                break;
            case R.id.rb_one_hour:
                loadKlineData(KlineTimeType.ONE_HOUR);
                break;
            case R.id.rb_one_day:
                loadKlineData(KlineTimeType.ONE_DAY);
                break;
            default:
                break;
        }
    }

    private OnClickListener optionClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            dialogOption.show();
        }
    };

    @Override
    public void share() {
        dp.show();
        DrawShareImageThread thread = new DrawShareImageThread();
        dp.setThread(thread);
        chartDepth.onlyClearTouch();
        chartKline.onlyClearTouch();
        marketDepthDetailView.setVisibility(View.INVISIBLE);
        mKLineDetailView.setVisibility(View.INVISIBLE);
        thread.start();
    }

    private class DrawShareImageThread extends Thread {
        @Override
        public void run() {
            Bitmap bmpContent = ImageManageUtil.getBitmapFromView(llContent);
            View v = LayoutInflater.from(MarketDetailActivity.this).inflate(
                    R.layout.layout_market_share_image, null);
            TextView tvTitle = (TextView) v.findViewById(R.id.tv_market_name);
            ImageView iv = (ImageView) v.findViewById(R.id.iv);
            tvTitle.setText(tvMarketName.getText());
            iv.setImageBitmap(bmpContent);
            int width = bmpContent.getWidth();
            int height = bmpContent.getHeight() + tvMarketName.getHeight() * 2;
            Bitmap result = ImageManageUtil.getBitmapFromView(v, width, height);
            final Uri uri = FileUtil.saveShareImage(result);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    if (uri != null) {
                        Intent intent = new Intent(
                                android.content.Intent.ACTION_SEND);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setType("image/jpg");
                        startActivity(intent);
                    } else {
                        DropdownMessage.showDropdownMessage(
                                MarketDetailActivity.this,
                                R.string.market_share_failed);
                    }
                }
            });
        }
    }

    @Override
    public Market getMarket() {
        return MarketUtil.getMarket(marketType);
    }

    private void setKlineRadioButtonEnabled(boolean enabled) {
        int count = rg.getChildCount();
        for (int i = 0;
             i < count;
             i++) {
            View v = rg.getChildAt(i);
            if (v instanceof RadioButton) {
                RadioButton rb = (RadioButton) v;
                rb.setEnabled(enabled);
            }
        }
    }

    private ITouchEventResponse marketDepthViewTouchResponse = new ITouchEventResponse() {

        @Override
        public void notifyTouchPointMove(int x, int y) {
            handler.removeCallbacks(disappearMarketDepthRunnable);
            marketDepthDetailView.notifyViewMove(x, y, chartDepth.getWidth());
            handler.postDelayed(disappearMarketDepthRunnable, DISAPPEAR_TIME);

        }

        @Override
        public void notifyTouchContentChange(Object[] objs) {
            String order = getString(R.string.buy_order);
            if (!Boolean.valueOf(objs[0].toString())) {
                order = getString(R.string.sell_order);
            }
            String priceString = objs[1].toString();
            String volumeString = objs[2].toString();

            marketDepthDetailView.setContent(order, priceString, volumeString);

        }

        @Override
        public void clearTounchPoint() {
            marketDepthDetailView.hide();

        }
    };
    private ITouchEventResponse kLineTouchResponse = new ITouchEventResponse() {

        @Override
        public void notifyTouchPointMove(int x, int y) {
            handler.removeCallbacks(disappearKlineRunnable);
            mKLineDetailView.notifyViewMove(x, y, chartKline.getWidth(),
                    chartKline.getHeight());
            handler.postDelayed(disappearKlineRunnable, DISAPPEAR_TIME);
        }

        @Override
        public void notifyTouchContentChange(Object[] objs) {
            Date date = new Date(Long.valueOf(objs[0].toString()));
            String dateString = DateTimeUtil.getShortDateTimeString(date);
            mKLineDetailView.setContent(dateString, objs[1].toString(),
                    objs[2].toString(), objs[3].toString(), objs[4].toString(),
                    objs[5].toString(), objs[6].toString(), objs[7].toString());

        }

        @Override
        public void clearTounchPoint() {
            mKLineDetailView.setVisibility(View.INVISIBLE);

        }

    };
    private Runnable disappearMarketDepthRunnable = new Runnable() {

        @Override
        public void run() {

            chartDepth.clearTounch();
            chartDepth.invalidate();

        }
    };
    private Runnable disappearKlineRunnable = new Runnable() {

        @Override
        public void run() {
            chartKline.clearTounch();
            chartKline.invalidate();
        }
    };
}
