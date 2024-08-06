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

package net.bither.service;

import static android.support.v4.app.NotificationCompat.CATEGORY_SERVICE;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Block;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.exception.BlockStoreException;
import net.bither.bitherj.utils.BlockUtil;
import net.bither.bitherj.utils.TransactionsUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.DownloadSpvRunnable;
import net.bither.util.BitherTimer;
import net.bither.util.BroadcastUtil;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.NetworkUtil.NetworkType;
import net.bither.util.UpgradeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class BlockchainService extends android.app.Service {

    public static final String ACTION_BEGIN_DOWLOAD_SPV_BLOCK = R.class
            .getPackage().getName() + ".dowload_block_api_begin";
    private static final Logger log = LoggerFactory
            .getLogger(BlockchainService.class);
    private WakeLock wakeLock;
    private long serviceCreatedAt;
    private BitherTimer mBitherTimer;
    private SPVFinishedReceiver spvFinishedReceiver = null;
    private TickReceiver tickReceiver = null;
    private TxReceiver txReceiver = null;

    private boolean connectivityReceivered = false;

    private final IBinder mBinder = new LocalBinder(BlockchainService.this);

    private boolean peerCanNotRun = false;

    @Override
    public void onCreate() {
        serviceCreatedAt = System.currentTimeMillis();
        log.info(".onCreate()");
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground();
        }
        final String lockName = getPackageName() + " blockchain sync";
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
            tickReceiver = new TickReceiver(BlockchainService.this);
            txReceiver = new TxReceiver(BlockchainService.this, tickReceiver);
            receiverConnectivity();
            registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
            registerReceiver(txReceiver, new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE));
            BroadcastUtil.sendBroadcastStartPeer();
        }
        startMarkTimerTask();
    }

    private void startForeground() {
        String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("kim.hsl", "ForegroundService");
        } else {
            channelId = "";
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        Notification notification = builder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.sync_block_data_title))
                .setContentText(getString(R.string.sync_block_data_des))
                .setPriority(PRIORITY_MIN)
                .setCategory(CATEGORY_SERVICE)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    private void receiverConnectivity() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        intentFilter
                .addAction(BroadcastUtil.ACTION_START_PEER_MANAGER);
        registerReceiver(connectivityReceiver, intentFilter);
        connectivityReceivered = true;
    }

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

    private void scheduleStartBlockchainService(@Nonnull final Context context) {
        BitherSetting.SyncInterval syncInterval = AppSharedPreference.getInstance().getSyncInterval();
        if (syncInterval == BitherSetting.SyncInterval.OnlyOpenApp ||
                AddressManager.getInstance().getAllAddresses().size() == 0) {
            return;
        }
        long interval = AlarmManager.INTERVAL_HOUR;
        if (syncInterval == BitherSetting.SyncInterval.Normal) {
            final long lastUsedAgo = AppSharedPreference.getInstance().getLastUsedAgo();
            if (lastUsedAgo < BitherSetting.LAST_USAGE_THRESHOLD_JUST_MS) {
                interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                log.info("start INTERVAL_FIFTEEN_MINUTES");
            } else if (lastUsedAgo < BitherSetting.LAST_USAGE_THRESHOLD_RECENTLY_MS) {
                interval = AlarmManager.INTERVAL_HALF_DAY;
                log.info("start INTERVAL_HALF_DAY");
            } else {
                interval = AlarmManager.INTERVAL_DAY;
                log.info("start INTERVAL_DAY");
            }
        }
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context
                .ALARM_SERVICE);
        final PendingIntent alarmIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmIntent = PendingIntent.getService(context, 0, new Intent(context, BlockchainService.class), PendingIntent.FLAG_IMMUTABLE|0);
        } else {
            alarmIntent = PendingIntent.getService(context, 0, new Intent(context, BlockchainService.class), 0);
        }
        alarmManager.cancel(alarmIntent);
        final long now = System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        // as of KitKat, set() is inexact
        {
            alarmManager.set(AlarmManager.RTC_WAKEUP, now + interval, alarmIntent);
        } else
        // workaround for no inexact set() before KitKat
        {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, now + interval,
                    AlarmManager.INTERVAL_HOUR, alarmIntent);
        }
    }

    @Override
    public void onDestroy() {
        log.info(".onDestroy()");
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
            stopForeground(true);
            scheduleStartBlockchainService(this);
            PeerManager.instance().stop();
            PeerManager.instance().onDestroy();
            if (mBitherTimer != null) {
                mBitherTimer.stopTimer();
                mBitherTimer = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
            if (connectivityReceivered) {
                unregisterReceiver(connectivityReceiver);
                connectivityReceivered = false;
            }
            if (tickReceiver != null) {
                unregisterReceiver(tickReceiver);
            }
            if (txReceiver != null) {
                unregisterReceiver(txReceiver);
            }
            BroadcastUtil.removeMarketState();
        }
        super.onDestroy();

        log.info("service was up for "
                + ((System.currentTimeMillis() - serviceCreatedAt) / 1000 / 60)
                + " minutes");
    }

    @Override
    public void onLowMemory() {
        log.warn("low memory detected, stopping service");
        stopSelf();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags,
                              final int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        final String action = intent.getAction();
        if (action != null) {
            LogUtil.i("onStartCommand", "onStartCommand Service:" + action);
        }
        if (ACTION_BEGIN_DOWLOAD_SPV_BLOCK.equals(action)) {
            new Thread(new DownloadSpvRunnable()).start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        log.debug(".onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        log.debug(".onUnbind()");

        return super.onUnbind(intent);
    }

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        private boolean hasConnectivity;
        private boolean hasStorage = true;

        @Override
        public void onReceive(final Context context, final Intent intent) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        onReceive(intent);
                    } catch (BlockStoreException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

        }

        private void onReceive(final Intent intent) throws BlockStoreException {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                hasConnectivity = !intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                log.info("network is " + (hasConnectivity ? "up" : "down"));
                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                hasStorage = false;
                log.info("device storage low");

                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                hasStorage = true;
                log.info("device storage ok");

                check();
            } else if (BroadcastUtil.ACTION_START_PEER_MANAGER
                    .equals(action)) {
                hasStorage = true;
                check();
            }
        }

        @SuppressLint("Wakelock")
        private void check() throws BlockStoreException {
            BitherjSettings.AppMode mode = AppSharedPreference.getInstance().getAppMode();
            if (mode == BitherjSettings.AppMode.COLD) {
                return;
            }
            final boolean hasEverything = hasConnectivity && hasStorage;
            NetworkType networkType = NetworkUtil.isConnectedType();
            boolean networkIsAvailadble = (!AppSharedPreference.getInstance().getSyncBlockOnlyWifi())
                    || (networkType == NetworkType.Wifi);

            if (networkIsAvailadble) {
                if (hasEverything) {
                    log.debug("acquiring wakelock");
                    callWekelock();
                    if (!PeerManager.instance().isRunning()) {
                        startPeer();
                    }
                } else {

                    PeerManager.instance().stop();
                }
            } else {

                PeerManager.instance().stop();
            }


        }
    };

    public void stopAndUnregister() {
        peerCanNotRun = true;
        if (connectivityReceivered) {
            unregisterReceiver(connectivityReceiver);
            connectivityReceivered = false;
        }
        PeerManager.instance().stop();
    }

    public void startAndRegister() {
        peerCanNotRun = false;
        receiverConnectivity();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startPeer();
            }
        }).start();
    }

    private void callWekelock() {
        if ((wakeLock != null) && // we have a WakeLock
                !wakeLock.isHeld()) { // but we don't hold it
            try {
                // WakeLock.acquireLocked(PowerManager.java:329) sdk16 nullpoint
                wakeLock.acquire();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private boolean spvFinishedReceivered = false;

    private synchronized void startPeer() {
        try {
            if (peerCanNotRun) {
                return;
            }
            if (UpgradeUtil.needUpgrade()) {
                return;
            }
            if (!AppSharedPreference.getInstance().getDownloadSpvFinish()) {
                Block block = BlockUtil.dowloadSpvBlock();
                if (block == null) {
                    return;
                }
            }
            PeerManager peerManager = PeerManager.instance();
            String customPeerDnsOrIp = AppSharedPreference.getInstance().getNetworkCustomPeerDnsOrIp();
            if (!Utils.isEmpty(customPeerDnsOrIp)) {
                peerManager.setCustomPeer(customPeerDnsOrIp, AppSharedPreference.getInstance().getNetworkCustomPeerPort());
            }
            if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
                if (!AppSharedPreference.getInstance().getBitherjDoneSyncFromSpv()) {
                    if (!peerManager.isConnected()) {
                        peerManager.start();
                        if (!spvFinishedReceivered) {
                            final IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(NotificationAndroidImpl.ACTION_SYNC_FROM_SPV_FINISHED);
                            spvFinishedReceiver = new SPVFinishedReceiver();
                            registerReceiver(spvFinishedReceiver, intentFilter);
                            spvFinishedReceivered = true;
                        }
                    }
                } else {
                    validStartPeerManager();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validStartPeerManager() {
        if (!AddressManager.getInstance().addressIsSyncComplete()) {
            if (TransactionsUtil.isReloading) {
                return;
            }
            try {
                TransactionsUtil.getMyTxFromBither();
                validStartPeerManager();
            } catch (Exception exception) {
                exception.printStackTrace();
                AbstractApp.notificationService.sendBroadcastAddressTxLoadError();
            }
        } else {
            startPeerManager();
        }
    }

    private void startPeerManager() {
        if (AddressManager.getInstance().addressIsSyncComplete()
                && AppSharedPreference.getInstance().getBitherjDoneSyncFromSpv()
                && AppSharedPreference.getInstance().getDownloadSpvFinish()) {
            NetworkType networkType = NetworkUtil.isConnectedType();
            boolean networkIsAvailadble = (!AppSharedPreference.getInstance().getSyncBlockOnlyWifi())
                    || (networkType == NetworkType.Wifi);
            if (networkIsAvailadble && !PeerManager.instance().isConnected()) {
                PeerManager.instance().start();
            }
        }

    }

    public void startMarkTimerTask() {
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
            if (mBitherTimer == null) {
                mBitherTimer = new BitherTimer(BlockchainService.this);
                mBitherTimer.startTimer();
            }
        }
    }


    public class SPVFinishedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d("block", "sendBroadcastSyncSPVFinished onReceive");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    spvFinishedReceiveValidStartPeerManager();
                }
            }).start();
        }
    }

    private void spvFinishedReceiveValidStartPeerManager() {
        try {
            if (!AddressManager.getInstance().addressIsSyncComplete()) {
                if (TransactionsUtil.isReloading) {
                    return;
                }
                TransactionsUtil.getMyTxFromBither();
                spvFinishedReceiveValidStartPeerManager();
                return;
            }
            startPeerManager();
            if (spvFinishedReceiver != null && spvFinishedReceivered) {
                unregisterReceiver(spvFinishedReceiver);
                spvFinishedReceivered = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!AddressManager.getInstance().addressIsSyncComplete()) {
                AbstractApp.notificationService.sendBroadcastAddressTxLoadError();
            }
        }
    }

}
