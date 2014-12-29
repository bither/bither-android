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

package net.bither;

import android.text.format.DateUtils;

import net.bither.bitherj.exception.AddressFormatException;

import java.nio.charset.Charset;


public class
        BitherSetting {

    public static final String DONATE_ADDRESS = "1BsTwoMaX3aYx9Nc8GdgHZzzAGmG669bC3";
    public static final long DONATE_AMOUNT = 100000;

    public static final int NOTIFICATION_ID_COINS_RECEIVED = 201451;
    public static final int NOTIFICATION_ID_NETWORK_ALERT = 201452;

    public static final double SYNC_PROGRESS_COMPLETE = 1.0;
    public static final double SYNC_TX_PROGRESS_BLOCK_HEIGHT = 0.1;
    public static final double SYNC_TX_PROGRESS_STEP1 = 0.6;
    public static final double SYNC_TX_PROGRESS_STEP2 = 0.1;
    public static final String UNKONW_ADDRESS_STRING = "---";
    public static final char CHAR_THIN_SPACE = '\u2009';
    public static final String CURRENCY_PLUS_SIGN = "+" + CHAR_THIN_SPACE;
    public static final String CURRENCY_MINUS_SIGN = "-" + CHAR_THIN_SPACE;

    public static final int MEMORY_CLASS_LOWEND = 48;

    public static final Charset UTF_8 = Charset.forName("UTF-8");


    public static final int WATCH_ONLY_ADDRESS_COUNT_LIMIT = 150;
    public static final int PRIVATE_KEY_OF_HOT_COUNT_LIMIT = 50;

    public static final int REQUEST_CODE_IMAGE = 1007;
    public static final int REQUEST_CODE_CAMERA = 1008;
    public static final int REQUEST_CODE_CROP_IMAGE = 1009;

    public static final long LAST_USAGE_THRESHOLD_JUST_MS = DateUtils.HOUR_IN_MILLIS;
    public static final long LAST_USAGE_THRESHOLD_RECENTLY_MS = 2 * DateUtils.DAY_IN_MILLIS;

    public class SwipeRightGesture {
        public static final int SCROLL_DELAY_HORIZONTAL = 75;
        public static final int SCROLL_DELAY_VERTICAL = 25;
        public static final int DISMISS_DISTANCE_DIVIDER = 10;

    }

    public class INTENT_REF {
        public static final int SCAN_REQUEST_CODE = 536;
        public static final int SIGN_TX_REQUEST_CODE = 253;
        public static final int CLONE_FROM_REQUEST_CODE = 1117;
        public static final int IMPORT_PRIVATE_KEY_REQUEST_CODE = 1356;
        public static final int IMPORT_BIP38PRIVATE_KEY_REQUEST_CODE = 1357;
        public static final int WIRELESS_SETTINGS_CODE = 537;
        public static final int SCAN_ALL_IN_BITHER_COLD_REUEST_CODE = 784;
        public static final String NOTIFICATION_ADDRESS = "tab_intent";
        public static final String ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG =
                "address_has_private_key_pass_value_tag";
        public static final String ADDRESS_POSITION_PASS_VALUE_TAG =
                "address_position_pass_value_tag";
        public static final String ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG = "add_private_key_suggest_check_tag";
        public static final String SCAN_ADDRESS_POSITION_TAG = "scan_address_position";
        public static final int SEND_REQUEST_CODE = 437;
        public static final String QR_CODE_STRING = "qr_code_string";
        public static final String OLD_QR_CODE_STRING = "old_qr_code_string";
        public static final String QR_CODE_HAS_CHANGE_ADDRESS_STRING = "qr_code_has_change_address";
        public static final String TITLE_STRING = "title_string";
        public static final String QRCODE_TYPE = "qrcode_type";
        public static final String MARKET_INTENT = "market_intnet";
        public static final String PIC_PASS_VALUE_TAG = "pic_pass_value";
        public static final String INTENT_FROM_NOTIF = "from_notif";
    }


    public enum KlineTimeType {
        ONE_MINUTE(1), FIVE_MINUTES(5), ONE_HOUR(60), ONE_DAY(1440);
        private int mVal;

        private KlineTimeType(int val) {
            this.mVal = val;
        }

        public int getValue() {
            return this.mVal;
        }
    }

    public enum MarketType {
        BITSTAMP(1), BTCE(2), HUOBI(3), OKCOIN(4), BTCCHINA(5), CHBTC(6), BITFINEX(7),
        MARKET796(8);
        private int mVal;

        private MarketType(int val) {
            this.mVal = val;
        }

        public int getValue() {
            return this.mVal;
        }
    }

//    public enum Currency {
//        USD("USD"), CNY("CNY"), EUR("EUR"), GBP("GBP"), JPY("JPY"), KRW("KRW"), CAD("CAD"), AUD("AUD");
//
//        private String mVal;
//
//        private Currency(String val) {
//            this.mVal = val;
//        }
//
//        public String getValue() {
//            return this.mVal;
//        }
//    }
//
//    public static Currency getCurrencyFromName(String name) {
//        if (name == null || name.length() == 0)
//            return Currency.USD;
//        if (name.equals("CNY"))
//            return Currency.CNY;
//        if (name.equals("EUR"))
//            return Currency.EUR;
//        if (name.equals("GBP"))
//            return Currency.GBP;
//        if (name.equals("JPY"))
//            return Currency.JPY;
//        if (name.equals("KRW"))
//            return Currency.KRW;
//        if (name.equals("CAD"))
//            return Currency.CAD;
//        if (name.equals("AUD"))
//            return Currency.AUD;
//        return Currency.USD;
//    }

    public enum AddressType {
        Normal, TxTooMuch, SpecialAddress
    }

    public static String getMarketName(MarketType marketType) {
        String name = "";
        switch (marketType) {
            case HUOBI:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_huobi);
                break;
            case BITSTAMP:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_bitstamp);
                break;
            case BTCE:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_btce);
                break;
            case OKCOIN:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_okcoin);
                break;
            case CHBTC:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_chbtc);
                break;
            case BTCCHINA:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_btcchina);
                break;
            case BITFINEX:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_bitfinex);
                break;
            case MARKET796:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_796);
                break;
            default:
                name = BitherApplication.mContext
                        .getString(R.string.market_name_bitstamp);
                break;
        }
        return name;
    }

    public enum QRCodeType {
        Bither, Bip38;

        public boolean checkFormat(String content) {
            switch (this) {
                case Bither:
                    //todo checkBitherQrCode
                    return true;

                case Bip38:
                    boolean check = false;
                    try {
                        check = net.bither.bitherj.crypto.bip38.Bip38.isBip38PrivateKey(content);
                    } catch (AddressFormatException e) {
                        e.printStackTrace();
                    }
                    return check;

            }
            return false;
        }

    }

    public enum SyncInterval {
        Normal(R.string.synchronous_interval_normal), OnlyOpenApp(R.string.synchronous_interval_only_open_app);

        private SyncInterval(int stringId) {
            this.stringId = stringId;
        }

        private int stringId;

        public int getStringId() {
            return stringId;
        }

    }
}
