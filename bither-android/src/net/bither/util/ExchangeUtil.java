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

import net.bither.BitherSetting.MarketType;
import net.bither.preference.AppSharedPreference;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.IOException;

public class ExchangeUtil {
    private ExchangeUtil() {

    }

    public enum ExchangeType {
        USD("$"), CNY(StringEscapeUtils.unescapeHtml("&yen;"));
        private String symbol;

        private ExchangeType(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private static double mRate = -1;

    public static void setExcchangeRate(double rate) throws IOException {
        mRate = rate;
        String rateString = Double.toString(rate);
        File file = FileUtil.getExchangeRateFile();
        FileUtil.writeFile(rateString.getBytes(), file);
    }

    public static double getExchangeRate() {
        if (mRate == -1) {
            File file = FileUtil.getExchangeRateFile();
            String rateString = FileUtil.readFile(file);
            if (StringUtil.isNubmer(rateString)) {
                mRate = Float.valueOf(rateString);
            } else {
                mRate = 1;
            }
        }
        return mRate;
    }

    public static double getRate(ExchangeType exchangeType) {
        ExchangeType defaultExchangeType = AppSharedPreference.getInstance()
                .getDefaultExchangeType();
        double rate = 1;
        if (exchangeType != defaultExchangeType) {
            double preRate = getExchangeRate();
            if (defaultExchangeType == ExchangeType.CNY) {
                rate = rate * preRate;
            } else {
                rate = rate / preRate;
            }
        }

        return rate;
    }

    public static double getRate(MarketType marketType) {
        ExchangeType exchangeType = AppSharedPreference.getInstance()
                .getDefaultExchangeType();
        double rate = 1;
        double preRate = getExchangeRate();
        switch (marketType) {
            case HUOBI:
            case OKCOIN:
            case BTCCHINA:
            case CHBTC:
                if (exchangeType == ExchangeType.USD) {
                    rate = rate / preRate;
                }
                break;
            case BTCE:
            case BITSTAMP:
                if (exchangeType == ExchangeType.CNY) {
                    rate = rate * preRate;
                }
                break;
            default:
                break;
        }
        if (rate < 0) {
            rate = 1;
        }
        return rate;

    }

    public static ExchangeType getExchangeType(MarketType marketType) {
        switch (marketType) {
            case HUOBI:
            case OKCOIN:
            case BTCCHINA:
            case CHBTC:
                return ExchangeType.CNY;
            case BTCE:
            case BITSTAMP:
                return ExchangeType.USD;
            default:
                break;
        }
        return ExchangeType.CNY;

    }

}
