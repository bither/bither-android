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
import net.bither.bitherj.utils.Threading;
import net.bither.exception.UEHandler;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.xrandom.URandom;

import org.slf4j.LoggerFactory;

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

    public void startBlockchainService() {
        startService(new Intent(mContext, BlockchainService.class));

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

    public static void scheduleStartBlockchainService(@Nonnull final Context context) {
        log.info("Schedule service restart after 15 minutes");
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context
                .ALARM_SERVICE);
        final PendingIntent alarmIntent = PendingIntent.getService(context, 0,
                new Intent(context, BlockchainService.class), 0);
        alarmManager.cancel(alarmIntent);
        final long now = System.currentTimeMillis();
        final long alarmInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        // as of KitKat, set() is inexact
        {
            alarmManager.set(AlarmManager.RTC_WAKEUP, now + alarmInterval, alarmIntent);
        } else
        // workaround for no inexact set() before KitKat
        {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, now + alarmInterval,
                    AlarmManager.INTERVAL_HOUR, alarmIntent);
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
