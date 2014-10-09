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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.ISetting;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.NotificationService;
import net.bither.bitherj.IRandom;
import net.bither.preference.AppSharedPreference;
import net.bither.xrandom.URandom;

import java.io.File;
import java.util.List;

public class AbstractAppAndroidImpl extends AbstractApp {

    @Override
    public IRandom initRandom() {
        return new URandom();
    }

    @Override
    public ISetting initSetting() {
        return new ISetting() {
            @Override
            public BitherjSettings.AppMode getAppMode() {
                return AppSharedPreference.getInstance().getAppMode();
            }

            @Override
            public boolean getBitherjDoneSyncFromSpv() {
                return AppSharedPreference.getInstance().getBitherjDoneSyncFromSpv();
            }

            @Override
            public void setBitherjDoneSyncFromSpv(boolean isDone) {
                AppSharedPreference.getInstance().setBitherjDoneSyncFromSpv(isDone);
            }

            @Override
            public BitherjSettings.TransactionFeeMode getTransactionFeeMode() {
                return AppSharedPreference.getInstance().getTransactionFeeMode();
            }

            @Override
            public File getPrivateDir(String dirName) {
                File file = BitherApplication.mContext.getDir(dirName, Context.MODE_PRIVATE);
                if (!file.exists()) {
                    file.mkdirs();
                }
                return file;
            }

            @Override
            public boolean isApplicationRunInForeground() {
                if (BitherApplication.mContext == null) {
                    return false;
                }
                ActivityManager am = (ActivityManager) BitherApplication.mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    ComponentName topActivity = tasks.get(0).topActivity;
                    if (!topActivity.getPackageName().equals(BitherApplication.mContext.getPackageName())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    @Override
    public NotificationService initNotification() {
        return new NotificationAndroidImpl();
    }
}
