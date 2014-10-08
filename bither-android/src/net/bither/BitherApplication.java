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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;

import net.bither.activity.cold.ColdActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.bitherj.BitherjApplication;
import net.bither.bitherj.ISetting;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.crypto.IRandom;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.Threading;
import net.bither.bitherj.utils.Utils;
import net.bither.exception.UEHandler;
import net.bither.image.glcrop.Util;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.util.LogUtil;
import net.bither.xrandom.URandom;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

public class BitherApplication extends BitherjApplication {

    private ActivityManager activityManager;

    private static org.slf4j.Logger log = LoggerFactory.getLogger(BitherApplication.class);
    private static BitherApplication mBitherApplication;
    public static HotActivity hotActivity;
    public static ColdActivity coldActivity;
    public static UEHandler ueHandler;
    public static Activity initialActivity;
    public static boolean isFirstIn = false;
    public static long reloadTxTime = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
                .permitDiskReads().permitDiskWrites().penaltyLog().build());
        Threading.throwOnLockCycles();
        mBitherApplication = this;
        ueHandler = new UEHandler();
        Thread.setDefaultUncaughtExceptionHandler(ueHandler);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    }


    public static BitherApplication getBitherApplication() {
        return mBitherApplication;
    }

    public static void startBlockchainService() {
        mContext.startService(new Intent(mContext, BlockchainService.class));

    }

    @Override
    public ISetting initSetting() {
        ISetting bitherjApp = new ISetting() {
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
                File file = mContext.getDir(dirName, Context.MODE_PRIVATE);
                if (!file.exists()) {
                    file.mkdirs();
                }
                return file;
            }

            @Override
            public boolean isApplicationRunInForeground() {
                if (mContext == null) {
                    return false;
                }
                ActivityManager am = (ActivityManager) mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    ComponentName topActivity = tasks.get(0).topActivity;
                    if (!topActivity.getPackageName().equals(mContext.getPackageName())) {
                        return false;
                    }
                }
                return true;
            }
        };
        return bitherjApp;
    }

    @Override
    public IRandom initRandom() {
        return new URandom();
    }

    public int maxConnectedPeers() {
        final int memoryClass = activityManager.getMemoryClass();
        if (memoryClass <= BitherSetting.MEMORY_CLASS_LOWEND) {
            return 4;
        } else {
            return 6;
        }
    }


    public static boolean canReloadTx() {
        if (reloadTxTime == -1) {
            return true;
        } else {
            return reloadTxTime + 60 * 60 * 1000 < System.currentTimeMillis();
        }
    }


}
