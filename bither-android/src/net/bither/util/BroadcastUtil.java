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

import android.content.Intent;

import net.bither.BitherApplication;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.model.Ticker;

import java.util.List;

public class BroadcastUtil {
    public static final String ACTION_START_PEER_MANAGER = R.class
            .getPackage().getName() + ".start_dowload_block_state";
    public static final String ACTION_ADDRESS_ERROR = "address_error";

    public static final String ACTION_MARKET = R.class.getPackage().getName()
            + ".market";

    public static final String ACTION_DOWLOAD_SPV_BLOCK = R.class.getPackage()
            .getName() + ".dowload_block_api_complete";
    public static final String ACTION_DOWLOAD_SPV_BLOCK_STATE = "complete";

    public static void sendBroadcastStartPeer() {
        final Intent broadcast = new Intent(ACTION_START_PEER_MANAGER);
        NotificationAndroidImpl.sendBroadcast(broadcast);
    }


    public static void sendBroadcastMarketState(List<Ticker> tickers) {
        if (tickers != null && tickers.size() > 0) {
            MarketUtil.setTickerList(tickers);
            final Intent broadcast = new Intent(ACTION_MARKET);
            NotificationAndroidImpl.sendBroadcast(broadcast);
        }
    }

    public static void removeMarketState() {
        BitherApplication.mContext.removeStickyBroadcast(new Intent(
                ACTION_MARKET));
    }

    public static void sendBroadcastGetSpvBlockComplete(boolean isComplete) {
        final Intent intent = new Intent(ACTION_DOWLOAD_SPV_BLOCK);
        intent.putExtra(ACTION_DOWLOAD_SPV_BLOCK_STATE, isComplete);
        NotificationAndroidImpl.sendBroadcast(intent);
        // LogUtil.d("broadcase", "sendBroadcastAddressGetBlockComplete");
    }

}
