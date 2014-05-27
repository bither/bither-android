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

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.bither.api.GetExchangeTickerApi;
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
                FileUtil.serializeObject(file, tickers);
                BroadcastUtil.sendBroadcastMarketState(tickers);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
