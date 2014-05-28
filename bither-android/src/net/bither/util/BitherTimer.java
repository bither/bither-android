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

package net.bither.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.MarketDetailActivity;
import net.bither.api.GetExchangeTickerApi;
import net.bither.model.PriceAlert;
import net.bither.model.Ticker;

public class BitherTimer {

    private Timer mTimer;
    private TimerTask mTimerTask;

    public void startTimer() {
        if (mTimer == null || mTimerTask == null) {
            mTimer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {

                    getExchangeTicker();
                }
            };
            if (mTimer != null && mTimerTask != null) {
                mTimer.schedule(mTimerTask, 0, 1 * 60 * 1000);
            }
        }

    }

    private void getExchangeTicker() {
        try {
            FileUtil.upgradeTickerFile();
            File file = FileUtil.getTickerFile();
            @SuppressWarnings("unchecked")
            List<Ticker> cacheList = (List<Ticker>) FileUtil.deserialize(file);
            if (cacheList != null) {
                BroadcastUtil.sendBroadcastMarketState(cacheList);
            }
            GetExchangeTickerApi getExchangeTickerApi = new GetExchangeTickerApi();
            getExchangeTickerApi.handleHttpGet();
            double exchangeRate = getExchangeTickerApi.getCurrencyRate();
            ExchangeUtil.setExcchangeRate(exchangeRate);
            List<Ticker> tickers = getExchangeTickerApi.getResult();
            if (tickers != null && tickers.size() > 0) {
                comporePriceAlert(tickers);
                FileUtil.serializeObject(file, tickers);
                BroadcastUtil.sendBroadcastMarketState(tickers);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void comporePriceAlert(List<Ticker> tickerList) {
        List<PriceAlert> priceAlertList = PriceAlert.getPriceAlertList();
        for (PriceAlert priceAlert : priceAlertList) {
            for (Ticker ticker : tickerList) {
                if (priceAlert.getMarketType() == ticker.getMarketType()) {
                    if (priceAlert.getHigher() > 0 && ticker.getDefaultExchangeHigh() >= priceAlert
                            .getHigher()) {
                        notif(ticker.getMarketType(), true, priceAlert.getHigher());
                    }
                    if (priceAlert.getLower() > 0 && ticker.getDefaultExchangeLow() <= priceAlert
                            .getLower()) {
                        notif(ticker.getMarketType(), false, priceAlert.getLower());
                    }
                }


            }
        }


    }

    private void notif(BitherSetting.MarketType marketType, boolean isHigher, double alertPrice) {
        Context context = BitherApplication.mContext;
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent2 = new Intent(BitherApplication.mContext, MarketDetailActivity.class);
        String title = context.getString(R.string.market_price_alert_title);
        String contentText;
        if (isHigher) {
            contentText = context.getString(R.string.price_alert_higher_than);
        } else {
            contentText = context.getString(R.string.price_alert_lower_than);
        }
        contentText = StringUtil.format(contentText, BitherSetting.getMarketName(marketType));
        contentText = contentText + StringUtil.formatDoubleToMoneyString(alertPrice);
        SystemUtil.nmNotifyDefault(nm, context,
                BitherSetting.NOTIFICATION_ID_NETWORK_ALERT, intent2,
                title, contentText, R.drawable.ic_launcher);
    }

}
