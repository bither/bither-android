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

package net.bither.activity.hot;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.adapter.hot.HotFragmentPagerAdapter;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.EnterpriseHDMAddress;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.utils.TransactionsUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.fragment.hot.MarketFragment;
import net.bither.fragment.hot.OptionHotFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.AddErrorMsgRunnable;
import net.bither.runnable.DownloadAvatarRunnable;
import net.bither.runnable.ThreadNeedService;
import net.bither.runnable.UploadAvatarRunnable;
import net.bither.service.BlockchainService;
import net.bither.ui.base.BaseFragmentActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SyncProgressView;
import net.bither.ui.base.TabButton;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogFirstRunWarning;
import net.bither.ui.base.dialog.DialogGenerateAddressFinalConfirm;
import net.bither.util.LogUtil;
import net.bither.util.NetworkUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static net.bither.NotificationAndroidImpl.ACTION_MINER_FEE_CHANGE;
import static net.bither.NotificationAndroidImpl.ACTION_UNSYNC_BLOCK_NUMBER_INFO;
import static net.bither.bitherj.core.PeerManager.ConnectedChangeBroadcast;

public class HotActivity extends BaseFragmentActivity {
    private TabButton tbtnMessage;
    private TabButton tbtnMain;
    private TabButton tbtnMe;
    private FrameLayout flAddAddress;
    private HotFragmentPagerAdapter mAdapter;
    private ViewPager mPager;
    private SyncProgressView pbSync;
    private LinearLayout llAlert;
    private TextView tvAlert;
    private ProgressBar pbAlert;

    private final TxAndBlockBroadcastReceiver txAndBlockBroadcastReceiver = new
            TxAndBlockBroadcastReceiver();
    private final ProgressBroadcastReceiver broadcastReceiver = new ProgressBroadcastReceiver();
    private final AddressIsLoadedReceiver addressIsLoadedReceiver = new AddressIsLoadedReceiver();
    private final AddressTxLoadingReceiver addressIsLoadingReceiver = new AddressTxLoadingReceiver();
    private final PeerConnectedChangeReceiver peerConnectedChangeReceiver = new PeerConnectedChangeReceiver();
    private final MinerFeeBroadcastReceiver minerFeeBroadcastReceiver = new MinerFeeBroadcastReceiver();

    protected void onCreate(Bundle savedInstanceState) {
        AbstractApp.notificationService.removeProgressState();
        AbstractApp.notificationService.removeAddressTxLoading();
        initAppState();
        super.onCreate(savedInstanceState);
        BitherApplication.hotActivity = this;
        setContentView(R.layout.activity_hot);
        initView();
        registerReceiver();
        mPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                initClick();
                mPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Fragment f = getActiveFragment();
                        if (f instanceof Selectable) {
                            ((Selectable) f).onSelected();
                        }
                    }
                }, 100);

                onNewIntent(getIntent());

            }
        }, 500);
        DialogFirstRunWarning.show(this);
        if (!NetworkUtil.isConnected()) {
            tvAlert.setText(R.string.tip_network_error);
            llAlert.setVisibility(View.VISIBLE);
        } else if (!AddressManager.getInstance().noAddress() && PeerManager.instance().getConnectedPeers().size() == 0) {
            tvAlert.setText(R.string.tip_no_peers_connected_scan);
            pbAlert.setVisibility(View.VISIBLE);
            llAlert.setVisibility(View.VISIBLE);
        }
    }

    private void registerReceiver() {
        registerReceiver(broadcastReceiver, new IntentFilter(NotificationAndroidImpl
                .ACTION_SYNC_BLOCK_AND_WALLET_STATE));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE);
        intentFilter.addAction(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE);
        registerReceiver(txAndBlockBroadcastReceiver, intentFilter);
        registerReceiver(addressIsLoadedReceiver,
                new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_LOAD_COMPLETE_STATE));
        registerReceiver(addressIsLoadingReceiver, new IntentFilter(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_STATE));
        registerReceiver(peerConnectedChangeReceiver, new IntentFilter(ConnectedChangeBroadcast));
        registerReceiver(minerFeeBroadcastReceiver, new IntentFilter(NotificationAndroidImpl.ACTION_MINER_FEE_CHANGE));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(txAndBlockBroadcastReceiver);
        unregisterReceiver(addressIsLoadedReceiver);
        unregisterReceiver(addressIsLoadingReceiver);
        unregisterReceiver(peerConnectedChangeReceiver);
        unregisterReceiver(minerFeeBroadcastReceiver);
        super.onDestroy();
        BitherApplication.hotActivity = null;

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BitherApplication.startBlockchainService();
        PeerManager.instance().notifyMaxConnectedPeerCountChange();
        refreshTotalBalance();
    }

    private void deleteNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel(BitherSetting.NOTIFICATION_ID_COINS_RECEIVED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        deleteNotification();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey
                (BitherSetting.INTENT_REF.NOTIFICATION_ADDRESS)) {
            final String address = intent.getExtras().getString(BitherSetting.INTENT_REF
                    .NOTIFICATION_ADDRESS);
            mPager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mPager.getCurrentItem() != 1) {
                        mPager.setCurrentItem(1, false);
                    }
                    Fragment fragment = getFragmentAtIndex(1);
                    if (fragment != null && fragment instanceof HotAddressFragment) {
                        ((HotAddressFragment) fragment).scrollToAddress(address);
                    }
                }
            }, 400);
        }
    }

    private void initView() {
        pbSync = (SyncProgressView) findViewById(R.id.pb_sync);
        flAddAddress = (FrameLayout) findViewById(R.id.fl_add_address);

        tbtnMain = (TabButton) findViewById(R.id.tbtn_main);
        tbtnMessage = (TabButton) findViewById(R.id.tbtn_message);
        tbtnMe = (TabButton) findViewById(R.id.tbtn_me);
        llAlert = findViewById(R.id.ll_alert);
        tvAlert = findViewById(R.id.tv_alert);
        pbAlert = findViewById(R.id.pb_alert);

        configureTopBarSize();
        configureTabMainIcons();
        tbtnMain.setBigInteger(null, null, null, null, null, null);
        if (AbstractApp.addressIsReady) {
            refreshTotalBalance();
        }
        tbtnMessage.setIconResource(R.drawable.tab_market, R.drawable.tab_market_checked);
        tbtnMe.setIconResource(R.drawable.tab_option, R.drawable.tab_option_checked);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter = new HotFragmentPagerAdapter(getSupportFragmentManager());
                mPager.setAdapter(mAdapter);
                mPager.setCurrentItem(1);
                mPager.setOffscreenPageLimit(2);
                mPager.setOnPageChangeListener(new PageChangeListener(new TabButton[]{tbtnMessage,
                        tbtnMain, tbtnMe}, mPager));
            }
        }, 100);
    }

    private void initClick() {
        flAddAddress.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean isPrivateKeyLimit = AddressManager.isPrivateLimit();
                boolean isWatchOnlyLimit = AddressManager.isWatchOnlyLimit();
                if (isPrivateKeyLimit && isWatchOnlyLimit) {
                    DropdownMessage.showDropdownMessage(HotActivity.this,
                            R.string.private_key_count_limit);
                    DropdownMessage.showDropdownMessage(HotActivity.this,
                            R.string.watch_only_address_count_limit);
                    return;
                }
                Intent intent = new Intent(HotActivity.this, AddHotAddressActivity.class);
                startActivityForResult(intent, BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
                overridePendingTransition(R.anim.activity_in_drop, R.anim.activity_out_back);

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> addresses = (ArrayList<String>) data.getExtras().getSerializable
                    (BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG);
            if (addresses != null && addresses.size() > 0) {
                Address a = WalletUtils.findPrivateKey(addresses.get(0));
                if (a != null && a.hasPrivKey() && !a.isFromXRandom()) {
                    new DialogGenerateAddressFinalConfirm(this, addresses.size(),
                            a.isFromXRandom()).show();
                }

                Fragment f = getFragmentAtIndex(1);
                if (f != null && f instanceof HotAddressFragment) {
                    mPager.setCurrentItem(1, true);
                    HotAddressFragment af = (HotAddressFragment) f;
                    af.showAddressesAdded(addresses);
                }
                if (f != null && f instanceof Refreshable) {
                    Refreshable r = (Refreshable) f;
                    r.doRefresh();
                }
            }
            return;
        }

        if (requestCode == SelectAddressToSendActivity.SEND_REQUEST_CODE && resultCode ==
                RESULT_OK) {
            DropdownMessage.showDropdownMessage(this, R.string.donate_thanks);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class PageChangeListener implements OnPageChangeListener {
        private List<TabButton> indicators;
        private ViewPager pager;

        public PageChangeListener(TabButton[] buttons, ViewPager viewPager) {
            this.indicators = new ArrayList<TabButton>();
            this.pager = viewPager;
            int size = buttons.length;
            for (int i = 0;
                 i < size;
                 i++) {
                TabButton button = buttons[i];
                indicators.add(button);
                if (pager.getCurrentItem() == i) {
                    button.setChecked(true);
                }
                button.setOnClickListener(new IndicatorClick(i));
            }

        }

        public void onPageScrollStateChanged(int state) {

        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        private class IndicatorClick implements OnClickListener {

            private int position;

            public IndicatorClick(int position) {
                this.position = position;
            }

            public void onClick(View v) {
                if (pager.getCurrentItem() != position) {
                    pager.setCurrentItem(position, true);
                } else {
                    if (getActiveFragment() instanceof Refreshable) {
                        ((Refreshable) getActiveFragment()).doRefresh();
                    }
                    if (position == 1) {
                        tbtnMain.showDialog();
                    }
                }
            }
        }

        public void onPageSelected(int position) {

            if (position >= 0 && position < indicators.size()) {
                for (int i = 0;
                     i < indicators.size();
                     i++) {
                    indicators.get(i).setChecked(i == position);
                    if (i != position) {
                        Fragment f = getFragmentAtIndex(i);
                        if (f instanceof Unselectable) {
                            ((Unselectable) f).onUnselected();
                        }
                    }
                }
            }
            Fragment mFragment = getActiveFragment();
            if (mFragment instanceof Selectable) {
                ((Selectable) mFragment).onSelected();
            }
        }
    }

    public void scrollToFragmentAt(int index) {
        if (mPager.getCurrentItem() != index) {
            mPager.setCurrentItem(index, true);
        }
    }

    private void configureTopBarSize() {
        int sideBarSize = UIUtil.getScreenWidth() / 3 - UIUtil.getScreenWidth() / 18;
        tbtnMessage.getLayoutParams().width = sideBarSize;
        tbtnMe.getLayoutParams().width = sideBarSize;
    }

    public Fragment getFragmentAtIndex(int i) {
        String str = StringUtil.makeFragmentName(this.mPager.getId(), i);
        return getSupportFragmentManager().findFragmentByTag(str);
    }

    public Fragment getActiveFragment() {
        Fragment localFragment = null;
        if (this.mPager == null) {
            return localFragment;
        }
        localFragment = getFragmentAtIndex(mPager.getCurrentItem());
        return localFragment;
    }

    private void initAppState() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppSharedPreference.getInstance().touchLastUsed();
                AddErrorMsgRunnable addErrorMsgRunnable = new AddErrorMsgRunnable();
                addErrorMsgRunnable.run();
                UploadAvatarRunnable uploadAvatarRunnable = new UploadAvatarRunnable();
                uploadAvatarRunnable.run();
                DownloadAvatarRunnable downloadAvatarRunnable = new DownloadAvatarRunnable();
                downloadAvatarRunnable.run();
            }
        }).start();
    }

    public void refreshTotalBalance() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long totalPrivate = 0;
                long totalWatchOnly = 0;
                long totalHdm = 0;
                long totalEnterpriseHdm = 0;
                for (Address address : AddressManager.getInstance().getPrivKeyAddresses()) {
                    totalPrivate += address.getBalance();
                }
                for (Address address : AddressManager.getInstance().getWatchOnlyAddresses()) {
                    totalWatchOnly += address.getBalance();
                }
                if (AddressManager.getInstance().hasHDMKeychain()) {
                    for (HDMAddress address : AddressManager.getInstance().getHdmKeychain()
                            .getAddresses()) {
                        totalHdm += address.getBalance();
                    }
                }
                if (AddressManager.getInstance().hasEnterpriseHDMKeychain()) {
                    for (EnterpriseHDMAddress address : AddressManager.getInstance()
                            .getEnterpriseHDMKeychain().getAddresses()) {
                        totalEnterpriseHdm += address.getBalance();
                    }
                }
                final long btcPrivate = totalPrivate;
                final long btcWatchOnly = totalWatchOnly;
                final long btcHdm = totalHdm;
                final long btcEnterpriseHdm = totalEnterpriseHdm;
                final long btcHD = AddressManager.getInstance().hasHDAccountHot() ? AddressManager
                        .getInstance().getHDAccountHot().getBalance() : 0;
                final long btcHdMonitored = AddressManager.getInstance().hasHDAccountMonitored()
                        ? AddressManager.getInstance().getHDAccountMonitored().getBalance() : 0;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        configureTabMainIcons();
                        tbtnMain.setBigInteger(BigInteger.valueOf(btcPrivate), BigInteger.valueOf
                                (btcWatchOnly), BigInteger.valueOf(btcHdm), BigInteger.valueOf
                                (btcHD), BigInteger.valueOf(btcHdMonitored),
                                BigInteger.valueOf(btcEnterpriseHdm));
                    }
                });
            }
        }).start();
    }

    private void configureTabMainIcons() {
        switch (AppSharedPreference.getInstance().getBitcoinUnit()) {
            case bits:
                tbtnMain.setIconResource(R.drawable.tab_main_bits,
                        R.drawable.tab_main_bits_checked);
                break;
            case BTC:
            default:
                tbtnMain.setIconResource(R.drawable.tab_main, R.drawable.tab_main_checked);
        }
    }

    public void notifPriceAlert(BitherjSettings.MarketType marketType) {
        if (mPager.getCurrentItem() != 0) {
            mPager.setCurrentItem(0);
        }
        Fragment fragment = getActiveFragment();
        if (fragment instanceof MarketFragment) {
            MarketFragment marketFragment = (MarketFragment) fragment;
            marketFragment.notifPriceAlert(marketType);
        }
    }

    private final class ProgressBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent != null) {
                if (intent.hasExtra(NotificationAndroidImpl.ACTION_PROGRESS_INFO)) {
                    double progress = intent.getDoubleExtra(NotificationAndroidImpl.ACTION_PROGRESS_INFO, 0);
                    LogUtil.d("progress", "BlockchainBroadcastReceiver" + progress);
                    pbSync.setProgress(progress);
                }
                if (intent.hasExtra(ACTION_UNSYNC_BLOCK_NUMBER_INFO)) {
                    long unsyncBlockNumber = intent.getLongExtra(ACTION_UNSYNC_BLOCK_NUMBER_INFO, 0);
                    if (unsyncBlockNumber > 0) {
                        tvAlert.setText(getString(R.string.tip_sync_block_height, unsyncBlockNumber));
                        if (llAlert.getVisibility() == View.GONE) {
                            pbAlert.setVisibility(View.VISIBLE);
                            llAlert.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (unsyncBlockNumber != -2) {
                            llAlert.setVisibility(View.GONE);
                        }
                    }
                }
            }
        }
    }

    private final class TxAndBlockBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    (!Utils.compareString(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE, intent.getAction())
                            && !Utils.compareString(NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE, intent.getAction()))) {
                return;
            }
            if (Utils.compareString(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE, intent.getAction())) {
                refreshTotalBalance();
            }
            Fragment fragment = getFragmentAtIndex(1);
            if (fragment != null && fragment instanceof HotAddressFragment) {
                ((HotAddressFragment) fragment).refresh();
            }
        }
    }

    private final class AddressIsLoadedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), NotificationAndroidImpl.ACTION_ADDRESS_LOAD_COMPLETE_STATE)) {
                return;
            }
            refreshTotalBalance();
            Fragment fragment = getFragmentAtIndex(1);
            if (fragment != null && fragment instanceof HotAddressFragment) {
                ((HotAddressFragment) fragment).refresh();
            }
        }
    }

    private final class AddressTxLoadingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_STATE)) {
                return;
            }
            if (intent.getBooleanExtra(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOAD_ERROR_INFO, false)) {
                TransactionsUtil.isReloading = false;
                addressTxLoadError();
                return;
            }
            if (!intent.hasExtra(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_INFO)) {
                return;
            }
            String address = intent.getStringExtra(NotificationAndroidImpl.ACTION_ADDRESS_TX_LOADING_INFO);
            if (Utils.isEmpty(address)) {
                llAlert.setVisibility(View.GONE);
                return;
            }
            tvAlert.setText(getString(R.string.tip_sync_address_tx, address));
            if (llAlert.getVisibility() == View.GONE) {
                pbAlert.setVisibility(View.VISIBLE);
                llAlert.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addressTxLoadError() {
        HotActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(HotActivity.this, getString(R.string.reload_tx_failed), getString(R.string.choose_mode_warm_retry), new Runnable() {
                    @Override
                    public void run() {
                        addressTxLoadRetry();
                    }
                }, false);
                dialogConfirmTask.setCanceledOnTouchOutside(false);
                dialogConfirmTask.show();
            }
        });
    }

    private void addressTxLoadRetry() {
        ThreadNeedService threadNeedService = new ThreadNeedService(null, HotActivity.this) {
            @Override
            public void runWithService(BlockchainService service) {
                try {
                    if (!AddressManager.getInstance().addressIsSyncComplete()) {
                        TransactionsUtil.getMyTxFromBither();
                    }
                    AbstractDb.peerProvider.recreate();
                    service.startAndRegister();
                } catch (Exception e) {
                    e.printStackTrace();
                    TransactionsUtil.isReloading = false;
                    addressTxLoadError();
                }
            }
        };
        threadNeedService.start();
    }

    private final class PeerConnectedChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), ConnectedChangeBroadcast)) {
                return;
            }
            if (!intent.hasExtra(ConnectedChangeBroadcast)) {
                return;
            }
            if (PeerManager.instance().getConnectedPeers().size() > 0 && llAlert.getVisibility() == View.VISIBLE && tvAlert.getText().toString().equals(getString(R.string.tip_no_peers_connected_scan))) {
                llAlert.setVisibility(View.GONE);
            }
        }
    }

    private final class MinerFeeBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Utils.compareString(intent.getAction(), ACTION_MINER_FEE_CHANGE)) {
                return;
            }
            Fragment fragment = getFragmentAtIndex(2);
            if (fragment instanceof OptionHotFragment) {
                ((OptionHotFragment) fragment).transactionFeeModeRefresh();
            }
        }
    }

//    private void addNewPrivateKey() {
//        final AppSharedPreference preference = AppSharedPreference.getInstance();
//        if (!preference.hasPrivateKey()) {
//            dp = new DialogProgress(HotActivity.this, R.string.please_wait);
//            dp.setCancelable(false);
//            DialogPassword dialogPassword = new DialogPassword(HotActivity.this,
//                    new DialogPasswordListener() {
//
//                        @Override
//                        public void onPasswordEntered(final SecureCharSequence password) {
//                            ThreadNeedService thread = new ThreadNeedService(dp, HotActivity.this) {
//
//                                @Override
//                                public void runWithService(BlockchainService service) {
//
//                                    ECKey ecKey = PrivateKeyUtil.encrypt(new ECKey(), password);
//                                    Address address = new Address(ecKey);
//                                    List<Address> addressList = new ArrayList<Address>();
//                                    addressList.add(address);
//                                    if (!AddressManager.getInstance().getAllAddresses().contains(address)) {
//
//                                        password.wipe();
//                                        KeyUtil.addAddressListByDesc(service, addressList);
//                                        preference.setHasPrivateKey(true);
//                                    }
//                                    password.wipe();
//                                    HotActivity.this.runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//
//                                            if (dp.isShowing()) {
//                                                dp.dismiss();
//                                            }
//                                            Fragment fragment = getFragmentAtIndex(1);
//                                            if (fragment instanceof Refreshable) {
//                                                ((Refreshable) fragment).doRefresh();
//                                            }
//
//                                            new DialogConfirmTask(HotActivity.this,
//                                                    getString(R.string
//                                                            .first_add_private_key_check_suggest),
//                                                    new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            ThreadUtil.runOnMainThread(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    Intent intent = new Intent(HotActivity.this,
//                                                                            CheckPrivateKeyActivity.class);
//                                                                    intent.putExtra(BitherSetting.INTENT_REF
//                                                                                    .ADD_PRIVATE_KEY_SUGGEST_CHECK_TAG, true
//                                                                    );
//                                                                    startActivity(intent);
//                                                                }
//                                                            });
//                                                        }
//                                                    }
//                                            ).show();
//                                        }
//                                    });
//                                }
//                            };
//                            thread.start();
//                        }
//                    }
//            );
//            dialogPassword.setCancelable(false);
//            dialogPassword.show();
//        }
//    }
}
