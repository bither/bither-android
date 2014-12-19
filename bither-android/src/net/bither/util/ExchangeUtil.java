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

import net.bither.BitherSetting;
import net.bither.BitherSetting.MarketType;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;

public class ExchangeUtil {
    private ExchangeUtil() {

    }

    public enum Currency {
        USD("USD", "$"),
        CNY("CNY", StringEscapeUtils.unescapeHtml("&yen;")),
        EUR("EUR", "€"),
        GBP("GBP", "£"),
        JPY("JPY", StringEscapeUtils.unescapeHtml("&yen;")),
        KRW("KRW", "₩"),
        CAD("CAD", "$"),
        AUD("AUD", "$");

        private String symbol;
        private String name;
        private Currency(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getName() {
            return name;
        }
    }

    private static double mRate = -1;
    private static AbstractMap<Currency, Double> mCurrenciesRate = null;

//    public static void setExchangeRate(double rate) throws IOException {
//        mRate = rate;
//        String rateString = Double.toString(rate);
//        File file = FileUtil.getExchangeRateFile();
//        Utils.writeFile(rateString.getBytes(), file);
//    }

//    public static double getExchangeRate() {
//        if (mRate == -1) {
//            File file = FileUtil.getExchangeRateFile();
//            String rateString = Utils.readFile(file);
//            if (Utils.isNubmer(rateString)) {
//                mRate = Float.valueOf(rateString);
//            } else {
//                mRate = 1;
//            }
//        }
//        return mRate;
//    }

    public static void setCurrenciesRate(JSONObject currenciesRateJSon) throws Exception {
        mCurrenciesRate = parseCurrenciesRate(currenciesRateJSon);
        File file = FileUtil.getCurrenciesRateFile();
        Utils.writeFile(currenciesRateJSon.toString().getBytes(), file);
    }

    public static AbstractMap<Currency, Double> getCurrenciesRate() {
        if (mCurrenciesRate == null) {
            File file = FileUtil.getCurrenciesRateFile();
            String rateString = Utils.readFile(file);
            try {
                if (!Utils.isEmpty(rateString)) {
                    JSONObject json = new JSONObject(rateString);
                    mCurrenciesRate = parseCurrenciesRate(json);
                }
            } catch (JSONException ex) {
                mCurrenciesRate = null;
            }
        }
        return mCurrenciesRate;
    }

    private static AbstractMap<Currency, Double> parseCurrenciesRate(JSONObject json) throws JSONException {
        HashMap<Currency, Double> currencyDoubleHashMap = new HashMap<Currency, Double>();
        currencyDoubleHashMap.put(Currency.USD, 1.0);
        for (Currency currency : Currency.values()) {
            if (!json.isNull(currency.getName())) {
                currencyDoubleHashMap.put(currency, json.getDouble(currency.getName()));
            }
        }
        return currencyDoubleHashMap;
    }

    public static double getRate(Currency currency) {
        Currency defaultCurrency = AppSharedPreference.getInstance()
                .getDefaultExchangeType();
        double rate = 1;
        if (currency != null && getCurrenciesRate() != null && currency != defaultCurrency) {
            double preRate = getCurrenciesRate().get(currency);
            double defaultRate = getCurrenciesRate().get(defaultCurrency);
            rate = defaultRate / preRate;
        }
        return rate;
    }

    public static double getRate(MarketType marketType) {
        Currency defaultCurrency = AppSharedPreference.getInstance()
                .getDefaultExchangeType();
        Currency currency = getExchangeType(marketType);
        double rate = 1;
        if (currency != null && getCurrenciesRate() != null && currency != defaultCurrency) {
            double preRate = getCurrenciesRate().get(currency);
            double defaultRate = getCurrenciesRate().get(defaultCurrency);
            rate = defaultRate / preRate;
        }
        return rate;
    }

    public static double getRate() {
        if (getCurrenciesRate() == null) {
            return 1.0;
        } else {
            Currency defaultCurrency = AppSharedPreference.getInstance()
                    .getDefaultExchangeType();
            return getCurrenciesRate().get(defaultCurrency);
        }
    }

    public static Currency getExchangeType(MarketType marketType) {
        switch (marketType) {
            case HUOBI:
            case OKCOIN:
            case BTCCHINA:
            case CHBTC:
                return Currency.CNY;
            case MARKET796:
            case BTCE:
            case BITSTAMP:
            case BITFINEX:
                return Currency.USD;
            default:
                break;
        }
        return Currency.CNY;

    }

}
