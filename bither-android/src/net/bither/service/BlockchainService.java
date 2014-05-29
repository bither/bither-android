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

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.BitherSetting.AppMode;
import net.bither.R;
import net.bither.activity.hot.HotActivity;
import net.bither.exception.NoAddressException;
import net.bither.model.BitherAddress;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.SyncBlockAndWalletMutiThread;
import net.bither.util.BitherTimer;
import net.bither.util.BroadcastUtil;
import net.bither.util.FileUtil;
import net.bither.util.GenericUtils;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.NetworkUtil.NetworkType;
import net.bither.util.SyncWalletUtil;
import net.bither.util.SystemUtil;
import net.bither.util.ThrottlingWalletChangeListener;
import net.bither.util.WalletUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.DateUtils;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionConfidence.Source;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;

public class BlockchainService extends android.app.Service {

    public static final String ACTION_PEER_STATE = BlockchainService.class
            .getPackage().getName() + ".peer_state";
    public static final String ACTION_PEER_STATE_NUM_PEERS = "num_peers";

    public static final String ACTION_BEGIN_DOWLOAD_SPV_BLOCK = R.class
            .getPackage().getName() + ".dowload_block_api_begin";

    public static final String ACTION_BLOCKCHAIN_STATE = BlockchainService.class
            .getPackage().getName() + ".blockchain_state";
    public static final String ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE = "best_chain_date";
    public static final String ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_HEIGHT = "best_chain_height";
    public static final String ACTION_BLOCKCHAIN_STATE_REPLAYING = "replaying";
    public static final String ACTION_BLOCKCHAIN_STATE_DOWNLOAD = "download";
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK = 0;
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM = 1;
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM = 2;

    public static final long BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS = DateUtils.SECOND_IN_MILLIS;
    public static final long BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;

    private static final int MIN_COLLECT_HISTORY = 2;
    private static final int IDLE_BLOCK_TIMEOUT_MIN = 2;
    private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 9;
    private static final int MAX_HISTORY_SIZE = Math.max(
            IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);

    public static final String ACTION_CANCEL_COINS_RECEIVED = R.class
            .getPackage().getName() + ".cancel_coins_received";
    public static final String ACTION_RESET_BLOCKCHAIN = R.class.getPackage()
            .getName() + ".reset_blockchain";
    public static final String ACTION_BROADCAST_TRANSACTION = R.class
            .getPackage().getName() + ".broadcast_transaction";
    public static final String ACTION_BROADCAST_TRANSACTION_HASH = "hash";

    private BitherApplication application;
    private AppSharedPreference prefs;
    private WakeLock wakeLock;
    private BlockStore blockStore;
    private File blockChainFile;
    private BlockChain blockChain;
    @CheckForNull
    private PeerGroup peerGroup;
    private final Handler handler = new Handler();
    private final Handler delayHandler = new Handler();

    private PeerConnectivityListener peerConnectivityListener;
    private NotificationManager nm;
    private HashMap<String, Integer> notificationHash = new HashMap<String, Integer>();

    private final List<Address> notificationAddresses = new LinkedList<Address>();
    private BigInteger notificationAccumulatedAmount = BigInteger.ZERO;
    private AtomicInteger transactionsReceived = new AtomicInteger();
    private int bestChainHeightEver;
    private long serviceCreatedAt;
    private boolean resetBlockchainOnShutdown = false;
    private BitherTimer mBitherTimer;

    private byte[] peerLock = new byte[0];

    private static final long APPWIDGET_THROTTLE_MS = DateUtils.SECOND_IN_MILLIS;

    private static final Logger log = LoggerFactory
            .getLogger(BlockchainService.class);

    private final WalletEventListener walletEventListener = new ThrottlingWalletChangeListener(
            APPWIDGET_THROTTLE_MS) {

        @Override
        public void onThrottledWalletChanged() {

        }

        @Override
        public void onCoinsReceived(final Wallet wallet, final Transaction tx,
                                    final BigInteger prevBalance, final BigInteger newBalance) {

            BroadcastUtil.sendBroadcastTotalBitcoinState(WalletUtils
                    .getTotalBitcoin());
            BroadcastUtil.sendBroadcastAddressState((BitherAddress) wallet);

            if (tx.getConfidence().getSource() == Source.SELF) {
                return;
            }
            transactionsReceived.incrementAndGet();
            final int bestChainHeight = blockChain.getBestChainHeight();

            try {

                final BigInteger amount = tx.getValue(wallet);
                final ConfidenceType confidenceType = tx.getConfidence()
                        .getConfidenceType();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        final boolean isReceived = amount.signum() > 0;
                        final boolean replaying = bestChainHeight <= bestChainHeightEver;
                        final boolean isReplayedTx = confidenceType == ConfidenceType.BUILDING
                                && replaying;

                        if (isReceived && !isReplayedTx) {
                            BitherAddress bit = (BitherAddress) wallet;
                            int height = -1;
                            if (tx.getConfidence().getConfidenceType() == ConfidenceType.BUILDING) {
                                height = tx.getConfidence()
                                        .getAppearedAtChainHeight();
                            }
                            notifyCoins(amount, true, bit.getAddress(), height);
                        }
                    }
                });

            } catch (final ScriptException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void onCoinsSent(final Wallet wallet, final Transaction tx,
                                final BigInteger prevBalance, final BigInteger newBalance) {

            BroadcastUtil.sendBroadcastTotalBitcoinState(WalletUtils
                    .getTotalBitcoin());
            BroadcastUtil.sendBroadcastAddressState((BitherAddress) wallet);

            if (tx.getConfidence().getSource() == Source.SELF) {
                return;
            }
            BitherAddress bit = (BitherAddress) wallet;
            BigInteger result = prevBalance;
            if (!newBalance.equals(BigInteger.ZERO)) {
                result = result.subtract(newBalance);
            }
            int height = -1;
            if (tx.getConfidence().getConfidenceType() == ConfidenceType.BUILDING) {
                height = tx.getConfidence().getAppearedAtChainHeight();
            }

            notifyCoins(result, false, bit.getAddress(), height);
            transactionsReceived.incrementAndGet();

        }

        @Override
        public void onScriptsAdded(Wallet arg0, List<Script> arg1) {

        }
    };

    private void notifyCoins(@Nonnull final BigInteger amount,
                             boolean isReceived, String address, int blockHeight) {
        notificationAccumulatedAmount = notificationAccumulatedAmount
                .add(amount);
        if (notificationHash.containsKey(address)) {
            int notificationHeight = notificationHash.get(address);
            if (blockHeight > 0 && blockHeight != notificationHeight
                    && notificationHeight != 0) {
                return;
            } else {
                notificationHash.remove(address);
                notificationHash.put(address, blockHeight);

            }
        }
        String contentText = address;
        String title = GenericUtils.formatValue(amount) + " BTC";
        if (isReceived) {
            title = getString(R.string.feed_received_btc) + " " + title;
        } else {
            title = getString(R.string.feed_send_btc) + " " + title;
        }
        Intent intent = new Intent(this, HotActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.NOTIFICATION_ADDRESS, address);
        SystemUtil.nmNotifyOfWallet(nm, BlockchainService.this,
                BitherSetting.NOTIFICATION_ID_COINS_RECEIVED, intent, title,
                contentText, R.drawable.ic_launcher, R.raw.coins_received);

    }

    private final class PeerConnectivityListener extends
            AbstractPeerEventListener {
        private int peerCount;
        private AtomicBoolean stopped = new AtomicBoolean(false);

        public PeerConnectivityListener() {

        }

        public void stop() {
            stopped.set(true);
        }

        @Override
        public void onPeerConnected(final Peer peer, final int peerCount) {
            this.peerCount = peerCount;
            changed(this.peerCount);
        }

        @Override
        public void onPeerDisconnected(final Peer peer, final int peerCount) {
            this.peerCount = peerCount;
            changed(this.peerCount);
        }

        @Override
        public void onTransaction(Peer peer, Transaction t) {
            if (t != null) {
                List<BitherAddress> list = WalletUtils
                        .getBitherAddressList(true);
                if (list != null) {
                    for (BitherAddress loopWallt : list) {
                        if (loopWallt != null) {
                            if (loopWallt.isTransactionRelevant(t)) {
                                if (!(t.isTimeLocked() && t.getConfidence()
                                        .getSource() != TransactionConfidence.Source.SELF)
                                        && loopWallt
                                        .isTransactionRisky(t, null)) {
                                    if (loopWallt.getTransaction(t.getHash()) == null) {
                                        loopWallt.receivePending(t, null);
                                        WalletUtils
                                                .notifyAddressInfo(loopWallt);

                                    }
                                }
                            }
                        }
                    }
                    BroadcastUtil.sendBroadcastTotalBitcoinState(WalletUtils
                            .getTotalBitcoin());
                }
            }
        }

        private void changed(final int numPeers) {
            if (stopped.get()) {
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {

                    sendBroadcastPeerState(numPeers);
                }
            });
        }
    }

    private void sendBroadcastPeerState(final int numPeers) {
        final Intent broadcast = new Intent(ACTION_PEER_STATE);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(ACTION_PEER_STATE_NUM_PEERS, numPeers);
        sendStickyBroadcast(broadcast);
    }

    private void removeBroadcastPeerState() {
        removeStickyBroadcast(new Intent(ACTION_PEER_STATE));
    }

    private void sendBroadcastBlockchainState(final int download) {
        if (blockChain != null) {
            final StoredBlock chainHead = blockChain.getChainHead();

            final Intent broadcast = new Intent(ACTION_BLOCKCHAIN_STATE);
            broadcast.setPackage(getPackageName());
            broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE,
                    chainHead.getHeader().getTime());
            broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_HEIGHT,
                    chainHead.getHeight());
            broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_REPLAYING,
                    chainHead.getHeight() < bestChainHeightEver);
            broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_DOWNLOAD, download);

            sendStickyBroadcast(broadcast);
        }
    }

    private void removeBroadcastBlockchainState() {
        removeStickyBroadcast(new Intent(ACTION_BLOCKCHAIN_STATE));
    }

    private final PeerEventListener blockchainDownloadListener = new AbstractPeerEventListener() {
        private final AtomicLong lastMessageTime = new AtomicLong(0);

        @Override
        public void onBlocksDownloaded(final Peer peer, final Block block,
                                       final int blocksLeft) {
            bestChainHeightEver = Math.max(bestChainHeightEver, blockChain
                    .getChainHead().getHeight());

            delayHandler.removeCallbacksAndMessages(null);

            final long now = System.currentTimeMillis();

            if (now - lastMessageTime.get() > BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS) {
                delayHandler.post(runnable);
            } else {
                delayHandler.postDelayed(runnable,
                        BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);
            }
            BitherApplication.updateChainHeight(blockChain);
        }

        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                lastMessageTime.set(System.currentTimeMillis());

                sendBroadcastBlockchainState(ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK);
            }
        };

    };

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        private boolean hasConnectivity;
        private boolean hasStorage = true;

        @Override
        public void onReceive(final Context context, final Intent intent) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    synchronized (peerLock) {
                        try {

                            onReceive(intent);

                        } catch (BlockStoreException e) {
                            e.printStackTrace();
                        }
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
            } else if (BroadcastUtil.ACTION_START_DOWLOAD_BLOCK_STATE
                    .equals(action)) {
                hasStorage = true;
                check();
            }
        }

        @SuppressLint("Wakelock")
        private void check() throws BlockStoreException {
            AppMode mode = prefs.getAppMode();
            if (mode == AppMode.COLD) {
                return;
            } else {
                if (!prefs.getDownloadSpvFinish()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            SyncWalletUtil
                                    .dowloadSpvStoredBlock(BlockchainService.this);
                        }
                    });
                    return;
                }
                if (mode == null) {
                    return;
                }
            }

            final boolean hasEverything = hasConnectivity && hasStorage;
            boolean needSyncWallet = false;
            boolean needRebuildBlock = false;
            try {
                needSyncWallet = SyncWalletUtil
                        .needGetTxFromApi(BlockchainService.this);
                needRebuildBlock = SyncWalletUtil
                        .needRebuildBlock(BlockchainService.this);
            } catch (NoAddressException e) {
                e.printStackTrace();
            }

            boolean noSyncWalletAndStoreBlock = !needRebuildBlock
                    && !needSyncWallet;
            NetworkType networkType = NetworkUtil.isConnectedType();
            boolean networkIsAvailadble = (!prefs.getSyncBlockOnlyWifi())
                    || (networkType == NetworkType.Wifi);

            if (noSyncWalletAndStoreBlock && networkIsAvailadble) {
                if (hasEverything && peerGroup == null && blockChain != null) {
                    log.debug("acquiring wakelock");
                    if ((wakeLock != null) && // we have a WakeLock
                            !wakeLock.isHeld()) { // but we don't hold it
                        wakeLock.acquire();
                    }
                    startPeerGroup();
                } else if (!hasEverything && peerGroup != null) {
                    stopPeerGroup();
                }
            } else {
                stopPeerGroup();
            }
            if (!noSyncWalletAndStoreBlock) {
                beginInitBlockAndWalletInUiThread();
            }
            final int download = (hasConnectivity ? 0
                    : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM)
                    | (hasStorage ? 0
                    : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM);
            sendBroadcastBlockchainState(download);
        }
    };

    public void startPeerGroup() {
        synchronized (peerLock) {
            log.info("starting peergroup");

            if (prefs.getAppMode() != AppMode.HOT) {
                log.info("not hot wallet");
                return;
            }

            List<BitherAddress> list = WalletUtils.getBitherAddressList(true);
            if (list == null || list.size() == 0) {
                log.info("address list empty");
                return;
            }
            peerGroup = new PeerGroup(BitherSetting.NETWORK_PARAMETERS,
                    blockChain);
            for (BitherAddress wallet : list) {
                peerGroup.addWallet(wallet);
            }
            peerGroup.setUserAgent(BitherSetting.USER_AGENT,
                    SystemUtil.packageInfo().versionName);
            peerGroup.addEventListener(peerConnectivityListener);

            final int maxConnectedPeers = application.maxConnectedPeers();
            peerGroup.setMaxConnections(maxConnectedPeers);

            peerGroup.addPeerDiscovery(new PeerDiscovery() {
                private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(
                        BitherSetting.NETWORK_PARAMETERS);

                @Override
                public InetSocketAddress[] getPeers(final long timeoutValue,
                                                    final TimeUnit timeoutUnit)
                        throws PeerDiscoveryException {
                    final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();

                    peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(
                            timeoutValue, timeoutUnit)));

                    return peers.toArray(new InetSocketAddress[0]);
                }

                @Override
                public void shutdown() {
                    normalPeerDiscovery.shutdown();
                }
            });

            peerGroup.start();
            peerGroup.startBlockChainDownload(blockchainDownloadListener);
        }
    }

    public void stopPeerGroup() {
        synchronized (peerLock) {
            if (peerGroup != null) {
                log.info("stopping peergroup");

                peerGroup.removeEventListener(peerConnectivityListener);
                List<BitherAddress> list = WalletUtils
                        .getBitherAddressList(true);
                if (list != null && list.size() > 0) {
                    for (BitherAddress wallet : list) {
                        peerGroup.removeWallet(wallet);
                    }
                }
                peerGroup.stop();
                peerGroup = null;

                log.debug("releasing wakelock");
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public BlockchainService getService() {
            return BlockchainService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

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

    @Override
    public void onCreate() {
        serviceCreatedAt = System.currentTimeMillis();
        log.info(".onCreate()");
        super.onCreate();
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = AppSharedPreference.getInstance();

        final String lockName = getPackageName() + " blockchain sync";
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
        application = (BitherApplication) getApplication();
        if (prefs.getAppMode() != AppMode.COLD) {
            if (!prefs.getDownloadSpvFinish()) {
                SyncWalletUtil.dowloadSpvStoredBlock(BlockchainService.this);
            }
            peerConnectivityListener = new PeerConnectivityListener();
            sendBroadcastPeerState(0);
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
            intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
            intentFilter
                    .addAction(BroadcastUtil.ACTION_START_DOWLOAD_BLOCK_STATE);
            registerReceiver(connectivityReceiver, intentFilter);
            initSyncBlockChain();
            try {
                if (!SyncWalletUtil.noConnectPeer(BlockchainService.this)) {
                    beginInitBlockAndWalletInUiThread();
                }
            } catch (NoAddressException e) {
                e.printStackTrace();
            } catch (BlockStoreException e) {
                e.printStackTrace();
            }
            registerReceiver(tickReceiver, new IntentFilter(
                    Intent.ACTION_TIME_TICK));
            startMarkTimerTask();
        }
    }

    private void addWalletOfBlockChain() {
        for (BitherAddress bit : WalletUtils.getBitherAddressList(true)) {
            blockChain.addWallet(bit);
        }
        if (WalletUtils.getBitherAddressList(true) != null) {
            for (BitherAddress wallet : WalletUtils.getBitherAddressList(true)) {
                wallet.removeEventListener(walletEventListener);
                wallet.addEventListener(walletEventListener);
            }
        }
    }

    private synchronized void syncBlockAndWallet() throws NoAddressException,
            BlockStoreException {
        if (prefs.getAppMode() != AppMode.HOT || !prefs.getDownloadSpvFinish()) {
            return;
        }
        if (SyncBlockAndWalletMutiThread.getInstance().isRunning()) {
            return;
        }
        if ((WalletUtils.getBitherAddressList(true) == null || WalletUtils
                .getBitherAddressList(true).size() == 0)) {
            return;
        }
        boolean needSyncWallet = SyncWalletUtil
                .needGetTxFromApi(BlockchainService.this);
        boolean needRebuildBlock = SyncWalletUtil
                .needRebuildBlock(BlockchainService.this);
        if (needSyncWallet) {
            SyncWalletUtil.syncWallet(BlockchainService.this);
        } else {
            if (needRebuildBlock) {
                SyncWalletUtil.reBuildBlock(BlockchainService.this,
                        blockChainFile);
            } else {
                initSyncBlockChain();
            }
        }
    }

    public void initSyncBlockChain() {
        blockChainFile = FileUtil.getBlockChainFile();
        if (blockChainFile.exists()) {
            try {
                blockStore = new SPVBlockStore(
                        BitherSetting.NETWORK_PARAMETERS, blockChainFile);
                blockStore.getChainHead();
                LogUtil.d("service", "blockStore h=" + getBlockStore());
                blockChain = new BlockChain(BitherSetting.NETWORK_PARAMETERS,
                        blockStore);
                BitherApplication.updateChainHeight(blockChain);
                addWalletOfBlockChain();
                BroadcastUtil.sendBroadcastDowloadBlockState();
            } catch (final BlockStoreException x) {
                blockChainFile.delete();
                prefs.setDownloadSpvFinish(false);
                final String msg = "blockstore cannot be created";
                log.error(msg, x);
                throw new Error(msg, x);
            }
        } else {
            prefs.setDownloadSpvFinish(false);
            SyncWalletUtil.dowloadSpvStoredBlock(BlockchainService.this);
        }
    }

    public void initSpvFile(List<StoredBlock> storedBlocks) {

        try {
            blockChainFile = FileUtil.getBlockChainFile();
            if (blockChainFile.exists()) {
                blockChainFile.delete();
            }
            blockStore = new SPVBlockStore(BitherSetting.NETWORK_PARAMETERS,
                    blockChainFile);
            for (StoredBlock storedBlock : storedBlocks) {
                blockStore.put(storedBlock);
                blockStore.setChainHead(storedBlock);
            }
            blockStore.getChainHead();
            blockChain = new BlockChain(BitherSetting.NETWORK_PARAMETERS,
                    blockStore);
            log.info("using " + blockStore.getClass().getName());
            BitherApplication.updateChainHeight(blockChain);
            addWalletOfBlockChain();

        } catch (BlockStoreException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(final Intent intent, final int flags,
                              final int startId) {
        final String action = intent.getAction();
        if (action != null) {
            log.info("onStartCommand Service:" + action);
        }
        if (ACTION_CANCEL_COINS_RECEIVED.equals(action)) {

            notificationAccumulatedAmount = BigInteger.ZERO;
            notificationAddresses.clear();

            nm.cancel(BitherSetting.NOTIFICATION_ID_COINS_RECEIVED);
        } else if (ACTION_RESET_BLOCKCHAIN.equals(action)) {
            log.info("will remove blockchain on service shutdown");

            resetBlockchainOnShutdown = true;
            stopSelf();
        } else if (ACTION_BROADCAST_TRANSACTION.equals(action)) {
            boolean hasPrivateKey = intent
                    .getBooleanExtra(
                            BitherSetting.INTENT_REF.ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG,
                            true);
            final Sha256Hash hash = new Sha256Hash(
                    intent.getByteArrayExtra(BlockchainService.ACTION_BROADCAST_TRANSACTION_HASH));
            int addressPosition = intent.getExtras().getInt(
                    BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG);
            Transaction tx;
            if (hasPrivateKey) {
                tx = WalletUtils.getPrivateAddressList().get(addressPosition)
                        .getTransaction(hash);
            } else {
                tx = WalletUtils.getWatchOnlyAddressList().get(addressPosition)
                        .getTransaction(hash);
            }
            if (peerGroup != null) {
                log.info("broadcasting transaction " + tx.getHashAsString());
                peerGroup.broadcastTransaction(tx);
            } else {
                log.info("peergroup not available, not broadcasting transaction "
                        + tx.getHashAsString());
            }
        } else if (ACTION_BEGIN_DOWLOAD_SPV_BLOCK.equals(action)) {
            SyncWalletUtil.dowloadSpvStoredBlock(BlockchainService.this);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        log.info(".onDestroy()");
        if (prefs.getAppMode() != AppMode.COLD) {
            if (WalletUtils.getBitherAddressList(true) != null) {
                for (BitherAddress bitherAddress : WalletUtils
                        .getBitherAddressList(true)) {
                    bitherAddress.removeEventListener(walletEventListener);
                }
            }

            if (peerGroup != null) {
                peerGroup.removeEventListener(peerConnectivityListener);
                if (WalletUtils.getBitherAddressList() != null) {
                    for (BitherAddress bitherAddress : WalletUtils
                            .getBitherAddressList(true)) {
                        peerGroup.removeWallet(bitherAddress);
                    }
                }
                peerGroup.stopAndWait();

                log.info("peergroup stopped");
            }

            peerConnectivityListener.stop();

            BitherApplication.scheduleStartBlockchainService(this);
            unregisterReceiver(tickReceiver);
            unregisterReceiver(connectivityReceiver);
            removeBroadcastBlockchainState();
            removeBroadcastPeerState();
            BroadcastUtil.removeProgressState();
            BroadcastUtil.removeAddressBitcoinState();
            BroadcastUtil.removeMarketState();
            try {
                if (blockStore != null) {
                    blockStore.close();
                }
            } catch (final BlockStoreException x) {
                throw new RuntimeException(x);
            }
            String str = "";
            if (WalletUtils.getBitherAddressList() != null) {
                for (BitherAddress bit : WalletUtils.getBitherAddressList()) {
                    str = str + bit.toString();
                }
            }
            LogUtil.i("save service", "destoy:" + str);
            WalletUtils.saveWallet();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }

            if (resetBlockchainOnShutdown) {
                log.info("removing blockchain");
                blockChainFile.delete();
            }
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

    public List<Peer> getConnectedPeers() {
        if (peerGroup != null) {
            return peerGroup.getConnectedPeers();
        } else {
            return null;
        }
    }

    public List<StoredBlock> getRecentBlocks(final int maxBlocks) {
        final List<StoredBlock> blocks = new ArrayList<StoredBlock>();

        try {
            StoredBlock block = blockChain.getChainHead();

            while (block != null) {
                blocks.add(block);

                if (blocks.size() >= maxBlocks) {
                    break;
                }

                block = block.getPrev(blockStore);
            }
        } catch (final BlockStoreException x) {
            // swallow
        }

        return blocks;
    }

    public int getMaxChainHeight() throws BlockStoreException {
        return getBlockStore().getHeight();
    }

    public StoredBlock getBlockStore() throws BlockStoreException {
        return blockStore.getChainHead();
    }


    public void beginInitBlockAndWalletInUiThread() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    syncBlockAndWallet();
                } catch (NoAddressException e) {
                    e.printStackTrace();
                } catch (BlockStoreException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private final static class ActivityHistoryEntry {
        public final int numTransactionsReceived;
        public final int numBlocksDownloaded;

        public ActivityHistoryEntry(final int numTransactionsReceived,
                                    final int numBlocksDownloaded) {
            this.numTransactionsReceived = numTransactionsReceived;
            this.numBlocksDownloaded = numBlocksDownloaded;
        }

        @Override
        public String toString() {
            return numTransactionsReceived + "/" + numBlocksDownloaded;
        }
    }

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        private int lastChainHeight = 0;
        private final List<ActivityHistoryEntry> activityHistory = new
                LinkedList<ActivityHistoryEntry>();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (prefs.getAppMode() == AppMode.COLD) {
                unregisterReceiver(tickReceiver);
                unregisterReceiver(connectivityReceiver);
                stopSelf();
                return;
            }
            if (prefs.getAppMode() == AppMode.HOT) {
                if (blockChain == null) {
                    return;
                }
                final int chainHeight = blockChain.getBestChainHeight();

                if (lastChainHeight > 0) {
                    final int numBlocksDownloaded = chainHeight
                            - lastChainHeight;
                    final int numTransactionsReceived = transactionsReceived
                            .getAndSet(0);

                    // push history
                    activityHistory.add(0, new ActivityHistoryEntry(
                            numTransactionsReceived, numBlocksDownloaded));

                    // trim
                    while (activityHistory.size() > MAX_HISTORY_SIZE) {
                        activityHistory.remove(activityHistory.size() - 1);
                    }

                    // print
                    final StringBuilder builder = new StringBuilder();
                    for (final ActivityHistoryEntry entry : activityHistory) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(entry);
                    }
                    log.info("History of transactions/blocks: " + builder);

                    // determine if block and transaction activity is idling
                    boolean isIdle = false;
                    if (activityHistory.size() >= MIN_COLLECT_HISTORY) {
                        isIdle = true;
                        for (int i = 0;
                             i < activityHistory.size();
                             i++) {
                            final ActivityHistoryEntry entry = activityHistory
                                    .get(i);
                            final boolean blocksActive = entry.numBlocksDownloaded > 0
                                    && i <= IDLE_BLOCK_TIMEOUT_MIN;
                            final boolean transactionsActive = entry.numTransactionsReceived > 0
                                    && i <= IDLE_TRANSACTION_TIMEOUT_MIN;

                            if (blocksActive || transactionsActive) {
                                isIdle = false;
                                break;
                            }
                        }
                    }

                    // if idling, shutdown service
                    if (isIdle) {
                        log.info("idling detected, stopping service");
                        stopSelf();
                    }
                }

                lastChainHeight = chainHeight;
            }
        }
    };

    public void startMarkTimerTask() {
        if (prefs.getAppMode() == AppMode.HOT) {
            if (mBitherTimer == null) {
                mBitherTimer = new BitherTimer();
                mBitherTimer.startTimer();
            }
        }
    }

    public void setDownloadSpvFinish(boolean finish) {
        prefs.setDownloadSpvFinish(finish);
    }

}
