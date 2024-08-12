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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
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
import net.bither.receiver.AutosyncReceiver;
import net.bither.runnable.DownloadSpvRunnable;
import net.bither.util.BitherTimer;
import net.bither.util.BroadcastUtil;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.UpgradeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class BlockchainService {

    private static BlockchainService instance;

    private static final Logger log = LoggerFactory
            .getLogger(BlockchainService.class);
    private PowerManager.WakeLock wakeLock;
    private BitherTimer mBitherTimer;
    private SPVFinishedReceiver spvFinishedReceiver = null;
    private TickReceiver tickReceiver = null;
    private TxReceiver txReceiver = null;

    private boolean connectivityReceivered = false;
    private boolean isRepeatingAlarmSet = false;

    private boolean peerCanNotRun = false;
    private Context mContext;

    public static BlockchainService getInstance() {
        if (instance == null) {
            instance = new BlockchainService();
        }
        return instance;
    }

    public void onStart() {
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            return;
        }
        if (mContext != null) {
            if (AppSharedPreference.getInstance().getSyncInterval() != BitherSetting.SyncInterval.OnlyOpenApp && !AddressManager.getInstance().noAddress()) {
                if (isRepeatingAlarmSet) {
                   return;
                }
                scheduleSync();
            } else {
                if (!isRepeatingAlarmSet) {
                    return;
                }
                scheduleSync();
            }
        }
        mContext = BitherApplication.mContext;
        final String lockName = mContext.getPackageName() + " blockchain sync";
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
        tickReceiver = new TickReceiver(this);
        txReceiver = new TxReceiver(mContext, tickReceiver);
        receiverConnectivity();
        registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(txReceiver, new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE));
        BroadcastUtil.sendBroadcastStartPeer();
        startMarkTimerTask();
        scheduleSync();
    }

    private void receiverConnectivity() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        intentFilter.addAction(BroadcastUtil.ACTION_START_PEER_MANAGER);
        registerReceiver(connectivityReceiver, intentFilter);
        connectivityReceivered = true;
    }

    private void registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.registerReceiver(receiver, filter);
        }
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        mContext.unregisterReceiver(receiver);
    }

    public void scheduleSync() {
        BitherSetting.SyncInterval syncInterval = AppSharedPreference.getInstance().getSyncInterval();
        PendingIntent alarmIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, AutosyncReceiver.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);
        if (syncInterval == BitherSetting.SyncInterval.OnlyOpenApp || AddressManager.getInstance().noAddress()) {
            isRepeatingAlarmSet = false;
            return;
        }
        long interval = 60 * 1000;
        final long now = System.currentTimeMillis();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, now + interval, interval, alarmIntent);
        isRepeatingAlarmSet = true;
    }

    public void onStop() {
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD && mContext != null) {
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
            mContext = null;
        }
    }

    public void downloadSpvBlock() {
        new Thread(new DownloadSpvRunnable()).start();
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
            NetworkUtil.NetworkType networkType = NetworkUtil.isConnectedType();
            boolean networkIsAvailadble = (!AppSharedPreference.getInstance().getSyncBlockOnlyWifi())
                    || (networkType == NetworkUtil.NetworkType.Wifi);

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
            NetworkUtil.NetworkType networkType = NetworkUtil.isConnectedType();
            boolean networkIsAvailadble = (!AppSharedPreference.getInstance().getSyncBlockOnlyWifi())
                    || (networkType == NetworkUtil.NetworkType.Wifi);
            if (networkIsAvailadble && !PeerManager.instance().isConnected()) {
                PeerManager.instance().start();
            }
        }

    }

    public void startMarkTimerTask() {
        if (AppSharedPreference.getInstance().getAppMode() != BitherjSettings.AppMode.COLD) {
            if (mBitherTimer == null) {
                mBitherTimer = new BitherTimer(mContext);
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
