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

package net.bither.receiver;

import net.bither.BitherSetting;
import net.bither.BitherSetting.AppMode;
import net.bither.ChooseModeActivity;
import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.ServiceUtil;
import net.bither.util.SystemUtil;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        LogUtil.d("receiver", action);
        if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
            if (NetworkUtil.isConnected() || NetworkUtil.BluetoothIsConnected()) {
                NotificationManager nm = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                Intent intent2 = new Intent(context, ChooseModeActivity.class);

                String title = context.getString(R.string.cold_warning);
                String contentText = context
                        .getString(R.string.safe_your_wallet);
                SystemUtil.nmNotifyDefault(nm, context,
                        BitherSetting.NOTIFICATION_ID_NETWORK_ALERT, intent2,
                        title, contentText, R.drawable.ic_launcher);
            }
        } else {
            if (NetworkUtil.isConnected()) {
                ServiceUtil.doMarkTimerTask(true);
            }
        }

    }
}
