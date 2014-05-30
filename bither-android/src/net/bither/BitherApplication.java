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

import java.io.File;

import javax.annotation.Nonnull;

import net.bither.activity.hot.HotActivity;
import net.bither.exception.UEHandler;
import net.bither.preference.AppSharedPreference;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DialogCropPhotoTransit;
import net.bither.util.BroadcastUtil;
import net.bither.util.LinuxSecureRandom;
import net.bither.util.LogUtil;
import net.bither.util.TransactionsUtil;
import net.bither.util.WalletUtils;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.utils.Threading;

public class BitherApplication extends Application {

    private ActivityManager activityManager;

    private static org.slf4j.Logger log = LoggerFactory
            .getLogger(BitherApplication.class);

    private Intent blockchainServiceIntent;
    private Intent blockchainServiceCancelCoinsReceivedIntent;
    private Intent blockchainServiceResetBlockchainIntent;

    private static BitherApplication mBitherApplication;

    public static int ChainHeight;

    public static Context mContext;
    public static HotActivity hotActivity;
    public static UEHandler ueHandler;
    public static Activity initialActivity;
    public static boolean isFirstIn = false;

    private boolean canStopMonitor = true;// TODO to be removed

    @Override
    public void onCreate() {
        new LinuxSecureRandom(); // init proper random number generator
        initLogging();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll().permitDiskReads().permitDiskWrites().penaltyLog()
                .build());
        Threading.throwOnLockCycles();

        super.onCreate();
        mContext = getApplicationContext();
        mBitherApplication = this;
        ueHandler = new UEHandler();
        Thread.setDefaultUncaughtExceptionHandler(ueHandler);

        LogUtil.i("application", "configuration: "
                + (BitherSetting.TEST ? "test" : "prod") + ", "
                + BitherSetting.NETWORK_PARAMETERS.getId());
        configureTransactionMinFee();
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        blockchainServiceIntent = new Intent(this, BlockchainService.class);
        blockchainServiceCancelCoinsReceivedIntent = new Intent(
                BlockchainService.ACTION_CANCEL_COINS_RECEIVED, null, this,
                BlockchainService.class);
        blockchainServiceResetBlockchainIntent = new Intent(
                BlockchainService.ACTION_RESET_BLOCKCHAIN, null, this,
                BlockchainService.class);

        BroadcastUtil.removeBroadcastTotalBitcoinState();
        BroadcastUtil.removeAddressLoadCompleteState(this);

        WalletUtils.initWalletList();

    }

    private void initLogging() {
        final File logDir = getDir("log",
                BitherSetting.TEST ? Context.MODE_WORLD_READABLE : MODE_PRIVATE);
        final File logFile = new File(logDir, "wallet.log");

        final LoggerContext context = (LoggerContext) LoggerFactory
                .getILoggerFactory();

        final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
        filePattern.setContext(context);
        filePattern
                .setPattern("%d{HH:mm:ss.SSS} [%thread] %logger{0} - %msg%n");
        filePattern.start();

        final RollingFileAppender<ILoggingEvent> fileAppender = new
                RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(context);
        fileAppender.setFile(logFile.getAbsolutePath());

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new
                TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir.getAbsolutePath()
                + "/wallet.%d.log.gz");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setEncoder(filePattern);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setTagEncoder(logcatTagPattern);
        logcatAppender.setEncoder(logcatPattern);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger log = context
                .getLogger(Logger.ROOT_LOGGER_NAME);
        log.addAppender(fileAppender);
        log.addAppender(logcatAppender);
        log.setLevel(Level.INFO);
    }

    public static BitherApplication getBitherApplication() {
        return mBitherApplication;
    }

    public void startBlockchainService(final boolean cancelCoinsReceived) {
        if (cancelCoinsReceived) {
            startService(blockchainServiceCancelCoinsReceivedIntent);
        } else {
            startService(blockchainServiceIntent);
        }
    }

    public void stopBlockchainService() {
        stopService(blockchainServiceIntent);
    }

    public void resetBlockchain() {
        // actually stops the service
        startService(blockchainServiceResetBlockchainIntent);
    }

    public final String applicationPackageFlavor() {
        final String packageName = getPackageName();
        final int index = packageName.lastIndexOf('_');

        if (index != -1) {
            return packageName.substring(index + 1);
        } else {
            return null;
        }
    }

    public int maxConnectedPeers() {
        final int memoryClass = activityManager.getMemoryClass();
        if (memoryClass <= BitherSetting.MEMORY_CLASS_LOWEND) {
            return 4;
        } else {
            return 6;
        }
    }

    public static void scheduleStartBlockchainService(
            @Nonnull final Context context) {
        log.info("Schedule service restart after 15 minutes");
        final AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        final PendingIntent alarmIntent = PendingIntent.getService(context, 0,
                new Intent(context, BlockchainService.class), 0);
        alarmManager.cancel(alarmIntent);
        final long now = System.currentTimeMillis();
        final long alarmInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        // as of KitKat, set() is inexact
        {
            alarmManager.set(AlarmManager.RTC_WAKEUP, now + alarmInterval,
                    alarmIntent);
        } else
        // workaround for no inexact set() before KitKat
        {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, now
                    + alarmInterval, AlarmManager.INTERVAL_HOUR, alarmIntent);
        }
    }

    public boolean isCanStopMonitor() {
        return canStopMonitor;
    }

    public void setCanStopMonitor(boolean canStopMonitor) {
        this.canStopMonitor = canStopMonitor;
        if (canStopMonitor == false) {
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    setCanStopMonitor(true);
                }
            }, 60000);
        }
    }

    public static void updateChainHeight(BlockChain blockChain) {
        if (blockChain != null) {
            ChainHeight = blockChain.getChainHead().getHeight();
        }
    }

    private void configureTransactionMinFee() {
        TransactionsUtil.configureMinFee(AppSharedPreference.getInstance()
                .getTransactionFeeMode().getMinFeeSatoshi());
    }


}
