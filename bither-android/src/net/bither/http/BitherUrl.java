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

package net.bither.http;

public class BitherUrl {

    public static final String BITHER_API_URL = "http://b.getcai.com/";
    public static final String BITHER_GET_COOKIE_URL = BITHER_API_URL
            + "api/v1/cookie";

    public static final String BITHER_Q_GETBLOCK_COUNT_URL = BITHER_API_URL
            + "api/v1/block/count";

    public static final String BITHER_BLOCKHEADER = BITHER_API_URL
            + "api/v1/block/%d/list";
    public static final String BITHER_Q_MYTRANSACTIONS = BITHER_API_URL
            + "api/v1/address/%s/transaction";
    public static final String BITHER_GET_SPVBLOCK_API = BITHER_API_URL
            + "api/v1/block/spv";
    public static final String BITHER_GET_ONE_SPVBLOCK_API = BITHER_API_URL +
            "api/v1/block/spv/one";
    public static final String BITHER_ERROR_API = BITHER_API_URL
            + "api/v1/error";
    public static final String BITHER_EXCHANGE_TICKER = BITHER_API_URL
            + "api/v1/exchange/ticker";
    public static final String BITHER_KLINE_URL = BITHER_API_URL
            + "api/v1/exchange/%d/kline/%d";
    public static final String BITHER_DEPTH_URL = BITHER_API_URL
            + "api/v1/exchange/%d/depth";
    public static final String BITHER_TREND_URL = BITHER_API_URL
            + "api/v1/exchange/%d/trend";
    public static final String BITHER_UPLOAD_AVATAR = BITHER_API_URL + "api/v1/avatar";


    // matket website
    public static final String BITSTAMP_URL = "https://www.bitstamp.net/";
    public static final String BTC_E_URL = "https://btc-e.com/";
    public static final String HUOBI_URL = "http://www.huobi.com/";
    public static final String OKCOIN_URL = "https://www.okcoin.com/";
    public static final String BTCCHINA_URL = "https://vip.btcchina.com/";
    public static final String CHBTC_URL = "https://www.chbtc.com/";

}
