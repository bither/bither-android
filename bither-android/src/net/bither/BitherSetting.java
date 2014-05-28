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

import java.math.BigInteger;
import java.nio.charset.Charset;

import android.text.format.DateUtils;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

public class BitherSetting {

    public static final boolean LOG_DEBUG = true;

    public static final boolean DEV_DEBUG = false;
    // true is in test network
    public static final boolean TEST = false;

    public static final int BTC_PRECISION = 4;
    public static final int SHOW_PROGRESS_BLOCK_COUNT = 10;

    public static final String DONATE_ADDRESS = "1BsTwoMaX3aYx9Nc8GdgHZzzAGmG669bC3";
    public static final BigInteger DONATE_AMOUNT = BigInteger.valueOf(100000);

    public static final int NOTIFICATION_ID_COINS_RECEIVED = 201451;
    public static final int NOTIFICATION_ID_NETWORK_ALERT = 201452;
    public static final int NOTIFICATION_ID_PRICE_ALERT = 201453;

    public static final int CHAIN_TX_PER_NUM = 50;
    public static final String CHAIN_OFFSET = "&offset=%d";

    public static final double SYNC_PROGRESS_COMPLETE = 1.0;

    public static final double SYNC_BLOCK_PROGRESS_BLOCK_HEIGHT = 0.1;
    public static final double SYNC_BLOCK_PROGRESS_SETP2 = 0.9;

    public static final double SYNC_TX_PROGRESS_BLOCK_HEIGHT = 0.1;
    public static final double SYNC_TX_PROGRESS_STEP1 = 0.6;
    public static final double SYNC_TX_PROGRESS_STEP2 = 0.1;
    public static final double SYNC_TX_PROGRESS_STEP3 = 0.2;

    public static final int IO_BUFFER_SIZE = 8 * 1024;

    public static final int MAX_DISTANCE_HIGH_OF_API_STORE = 100;

    public static final String UNKONW_ADDRESS_STRING = "---";

    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params
            .get() : MainNetParams.get();
    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS
            .getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";

    public static final String BLOCKCHAIN_FILENAME = "blockchain"
            + FILENAME_NETWORK_SUFFIX;
    public static final String BLOCKCHAIN_FOLDER = "blockstore";

    public static final String MIMETYPE_TRANSACTION = "application/x-btctx";

    public static final String USER_AGENT = "Bither";

    public static final char CHAR_HAIR_SPACE = '\u200a';
    public static final char CHAR_THIN_SPACE = '\u2009';
    public static final char CHAR_ALMOST_EQUAL_TO = '\u2248';
    public static final String CURRENCY_PLUS_SIGN = "+" + CHAR_THIN_SPACE;
    public static final String CURRENCY_MINUS_SIGN = "-" + CHAR_THIN_SPACE;
    public static final String PREFIX_ALMOST_EQUAL_TO = Character
            .toString(CHAR_ALMOST_EQUAL_TO) + CHAR_THIN_SPACE;
    public static final int HTTP_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;

    public static final int MEMORY_CLASS_LOWEND = 48;

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    public static final int WATCH_ONLY_ADDRESS_COUNT_LIMIT = 100;
    public static final int PRIVATE_KEY_OF_HOT_COUNT_LIMIT = 10;

    public static final int REQUEST_CODE_IMAGE = 1007;
    public static final int REQUEST_CODE_CAMERA = 1008;
    public static final int REQUEST_CODE_CROP_IMAGE = 1009;

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
        public static final int WIRELESS_SETTINGS_CODE = 537;
        public static final int SCAN_ALL_IN_BITHER_COLD_REUEST_CODE = 784;
        public static final String NOTIFICATION_ADDRESS = "tab_intent";
        public static final String ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG =
                "address_has_private_key_pass_value_tag";
        public static final String ADDRESS_POSITION_PASS_VALUE_TAG =
                "address_position_pass_value_tag";
        public static final String SCAN_ADDRESS_POSITION_TAG = "scan_address_position";
        public static final int SEND_REQUEST_CODE = 437;
        public static final String PRIVATE_KEY_PASSWORD = "private_key_password";
        public static final String QR_CODE_STRING = "qr_code_string";
        public static final String TITLE_STRING = "title_string";
        public static final String MARKET_INTENT = "market_intnet";
        public static final String PIC_PASS_VALUE_TAG = "pic_pass_value";
    }

    public static enum AppMode {
        COLD, HOT
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
        BITSTAMP(1), BTCE(2), HUOBI(3), OKCOIN(4), BTCCHINA(5), CHBTC(6);
        private int mVal;

        private MarketType(int val) {
            this.mVal = val;
        }

        public int getValue() {
            return this.mVal;
        }
    }

}
