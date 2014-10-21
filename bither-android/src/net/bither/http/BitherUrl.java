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

import net.bither.util.StringUtil;

public class BitherUrl {
    public static final class BITHER_DNS {
        private static final String FOTMAT_HTTP = "http://%s/";

        public static final String BITHER_BITCOIN_DOMAIN = "b.getcai.com";
        public static final String BITHER_USER_DOMAIN = "bu.getcai.com";
        public static final String BITHER_STATS_DOMAIN = "bs.getcai.com";

        public static final String BITHER_BITCOIN = StringUtil.format(FOTMAT_HTTP, BITHER_BITCOIN_DOMAIN);
        public static final String BITHER_USER = StringUtil.format(FOTMAT_HTTP, BITHER_USER_DOMAIN);
        public static final String BITHER_STATS = StringUtil.format(FOTMAT_HTTP, BITHER_STATS_DOMAIN);


    }

    //bither user
    public static final String BITHER_GET_COOKIE_URL = BITHER_DNS.BITHER_USER
            + "api/v1/cookie";
    public static final String BITHER_UPLOAD_AVATAR = BITHER_DNS.BITHER_USER + "api/v1/avatar";
    public static final String BITHER_DOWNLOAD_AVATAR = BITHER_DNS.BITHER_USER + "api/v1/avatar";
    public static final String BITHER_ERROR_API = BITHER_DNS.BITHER_USER
            + "api/v1/error";

    //bither bitcoin
    public static final String BITHER_Q_GETBLOCK_COUNT_URL = BITHER_DNS.BITHER_BITCOIN
            + "api/v1/block/count";
    public static final String BITHER_BLOCKHEADER = BITHER_DNS.BITHER_BITCOIN
            + "api/v1/block/%d/list";
    public static final String BITHER_Q_MYTRANSACTIONS = BITHER_DNS.BITHER_BITCOIN
            + "api/v1/address/%s/transaction";
    public static final String BITHER_GET_SPVBLOCK_API = BITHER_DNS.BITHER_BITCOIN
            + "api/v1/block/spv";
    public static final String BITHER_GET_ONE_SPVBLOCK_API = BITHER_DNS.BITHER_BITCOIN +
            "api/v1/block/spv/one";
    public static final String BITHER_IN_SIGNATURES_API = BITHER_DNS.BITHER_BITCOIN + "api/v1/address/%s/insignature/%d";

    //bither stats
    public static final String BITHER_EXCHANGE_TICKER = BITHER_DNS.BITHER_STATS
            + "api/v1/exchange/ticker";
    public static final String BITHER_KLINE_URL = BITHER_DNS.BITHER_STATS
            + "api/v1/exchange/%d/kline/%d";
    public static final String BITHER_DEPTH_URL = BITHER_DNS.BITHER_STATS
            + "api/v1/exchange/%d/depth";
    public static final String BITHER_TREND_URL = BITHER_DNS.BITHER_STATS
            + "api/v1/exchange/%d/trend";

}
