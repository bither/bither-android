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

import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.BitherjSettings.MarketType;
import net.bither.model.Market;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;

import java.util.ArrayList;
import java.util.List;

public class MarketUtil {
    private static ArrayList<Market> markets = new ArrayList<Market>();

    public static ArrayList<Market> getMarkets() {
        synchronized (markets) {
            if (markets.size() == 0) {
                for (MarketType marketType : MarketType.values()) {
                    markets.add(new Market(marketType));
                }
            }
            return markets;
        }
    }

    public static Market getMarket(MarketType marketType) {
        if (markets.size() == 0) {
            getMarkets();
        }
        synchronized (markets) {

            if (markets.size() > 0) {
                for (Market market : markets) {
                    if (market.getMarketType() == marketType) {
                        return market;
                    }
                }
            }
            return null;
        }

    }

    public static Market getDefaultMarket() {
        BitherjSettings.MarketType marketType = AppSharedPreference.getInstance()
                .getDefaultMarket();
        Market market = getMarket(marketType);
        return market;
    }

    public static Ticker getTickerOfDefaultMarket() {
        Market market = getDefaultMarket();
        if (market != null) {
            return market.getTicker();
        }
        return null;

    }

    public static void setTickerList(List<Ticker> tickerList) {
        if (tickerList != null && tickerList.size() > 0) {
            synchronized (markets) {
                for (Ticker ticker : tickerList) {
                    Market market = getMarket(ticker.getMarketType());
                    if (market != null) {
                        market.setTicker(ticker);
                    }
                }
            }
        }

    }
}
