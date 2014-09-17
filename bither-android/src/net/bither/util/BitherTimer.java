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

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.hot.MarketDetailActivity;
import net.bither.api.GetExchangeTickerApi;
import net.bither.model.PriceAlert;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;

import java.io.File;
import java.util.List;

public class BitherTimer {
    private Thread thread = null;
    private Context context;
    private boolean isPause = false;

    public BitherTimer(Context context) {
        this.context = context;
    }

    public void startTimer() {
        if (thread == null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isPause) {
                        getExchangeTicker();
                        try {
                            Thread.sleep(1 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    LogUtil.d("bitherTime", "running");
                }
            });
            thread.start();
        }

    }

    public void pauseTimer() {
        isPause = true;
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
            ExchangeUtil.setExchangeRate(exchangeRate);
            List<Ticker> tickers = getExchangeTickerApi.getResult();
            if (tickers != null && tickers.size() > 0) {
                comparePriceAlert(tickers);
                FileUtil.serializeObject(file, tickers);
                BroadcastUtil.sendBroadcastMarketState(tickers);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void comparePriceAlert(List<Ticker> tickerList) {
        LogUtil.d("price", "comparePriceAlert:" + tickerList.size());
        List<PriceAlert> priceAlertList = PriceAlert.getPriceAlertList();
        for (PriceAlert priceAlert : priceAlertList) {
            for (Ticker ticker : tickerList) {
                if (priceAlert.getMarketType() == ticker.getMarketType()) {
                    if (priceAlert.getExchangeHigher() > 0 && ticker.getDefaultExchangePrice() >=
                            priceAlert
                                    .getExchangeHigher()) {
                        notif(ticker.getMarketType(), true, priceAlert.getExchangeHigher());
                        priceAlert.setHigher(-1);
                        PriceAlert.removePriceAlert(priceAlert);
                    }
                    if (priceAlert.getExchangeLower() > 0 && ticker.getDefaultExchangePrice() <=
                            priceAlert
                                    .getExchangeLower()) {
                        notif(ticker.getMarketType(), false, priceAlert.getExchangeLower());
                        priceAlert.setLower(-1);
                        PriceAlert.removePriceAlert(priceAlert);
                    }
                }


            }
        }


    }

    private void notif(final BitherSetting.MarketType marketType, boolean isHigher,
                       double alertPrice) {
        LogUtil.d("price", "notif");
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, MarketDetailActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.MARKET_INTENT, marketType);
        intent.putExtra(BitherSetting.INTENT_REF.INTENT_FROM_NOTIF, true);
        String title = context.getString(R.string.market_price_alert_title);
        String contentText;
        if (isHigher) {
            contentText = context.getString(R.string.price_alert_higher_than);
        } else {
            contentText = context.getString(R.string.price_alert_lower_than);
        }
        contentText = StringUtil.format(contentText, BitherSetting.getMarketName(marketType));
        contentText = contentText + " " + AppSharedPreference.getInstance()
                .getDefaultExchangeType().getSymbol() + StringUtil.formatDoubleToMoneyString
                (alertPrice);
        SystemUtil.nmNotifyDefault(nm, context,
                BitherSetting.NOTIFICATION_ID_NETWORK_ALERT, intent,
                title, contentText, R.drawable.ic_launcher);

    }

}
