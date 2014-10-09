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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.bither.BitherApplication;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.util.LogUtil;

public class AutosyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) {
            return;
        }
        LogUtil.d("receiver", intent.getAction());
        // make sure there is always an alarm scheduled
        if (!Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())
                || (Utils.compareString(intent.getDataString(), "package:" + context.getPackageName()))) {
            if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                BitherApplication.getBitherApplication()
                        .startBlockchainService();
            }
        }
    }
}
