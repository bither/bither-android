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

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.adapter.hot.HotFragmentPagerAdapter;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.fragment.hot.MarketFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.AddErrorMsgRunnable;
import net.bither.runnable.DownloadAvatarRunnable;
import net.bither.runnable.UploadAvatarRunnable;
import net.bither.ui.base.BaseFragmentActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SyncProgressView;
import net.bither.ui.base.TabButton;
import net.bither.ui.base.dialog.DialogFirstRunWarning;
import net.bither.ui.base.dialog.DialogGenerateAddressFinalConfirm;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.util.LogUtil;
import net.bither.util.ServiceUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class HotActivity extends BaseFragmentActivity {
    private TabButton tbtnMessage;
    private TabButton tbtnMain;
    private TabButton tbtnMe;
    private FrameLayout flAddAddress;
    private HotFragmentPagerAdapter mAdapter;
    private ViewPager mPager;
    private SyncProgressView pbSync;
    private DialogProgress dp;

    private final TxAndBlockBroadcastReceiver txAndBlockBroadcastReceiver = new
            TxAndBlockBroadcastReceiver();
    private final ProgressBroadcastReceiver broadcastReceiver = new ProgressBroadcastReceiver();
    private final AddressIsLoadedReceiver addressIsLoadedReceiver = new AddressIsLoadedReceiver();

    protected void onCreate(Bundle savedInstanceState) {
        AbstractApp.notificationService.removeProgressState();
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
                mPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showNewCount();
                    }
                }, 500);
                onNewIntent(getIntent());

            }
        }, 500);
//        mPager.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (ServiceUtil.localTimeIsWrong()) {
//                    DropdownMessage.showDropdownMessage(HotActivity.this, R.string.time_is_wrong);
//                }
//            }
//        }, 2 * 1000);
        DialogFirstRunWarning.show(this);
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
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(txAndBlockBroadcastReceiver);
        unregisterReceiver(addressIsLoadedReceiver);
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

        configureTopBarSize();

        configureTabMainIcons();
        tbtnMain.setBigInteger(null, null);
        if (AbstractApp.addressIsReady) {
            refreshTotalBalance();
        }
        tbtnMessage.setIconResource(R.drawable.tab_market, R.drawable.tab_market_checked);
        tbtnMe.setIconResource(R.drawable.tab_option, R.drawable.tab_option_checked);

        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new HotFragmentPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1);
        mPager.setOffscreenPageLimit(2);
        mPager.setOnPageChangeListener(new PageChangeListener(new TabButton[]{tbtnMessage,
                tbtnMain, tbtnMe}, mPager));
    }

    private void initClick() {
        flAddAddress.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean isPrivateKeyLimit = WalletUtils.isPrivateLimit();
                boolean isWatchOnlyLimit = WalletUtils.isWatchOnlyLimit();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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
            showNewCount();
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

    public void showNewCount() {

    }

    public void refreshTotalBalance() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long toatlPrivate = 0;
                long toatlWatchOnly = 0;
                for (Address address : AddressManager.getInstance().getPrivKeyAddresses()) {
                    toatlPrivate += address.getBalance();
                }
                for (Address address : AddressManager.getInstance().getWatchOnlyAddresses()) {
                    toatlWatchOnly += address.getBalance();
                }
                final long btcPrivate = toatlPrivate;
                final long btcWatchOnly = toatlWatchOnly;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        configureTabMainIcons();
                        tbtnMain.setBigInteger(BigInteger.valueOf(btcPrivate), BigInteger.valueOf(btcWatchOnly));
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

    public void notifPriceAlert(BitherSetting.MarketType marketType) {
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
            if (intent != null && intent.hasExtra(NotificationAndroidImpl.ACTION_PROGRESS_INFO)) {
                double progress = intent.getDoubleExtra(NotificationAndroidImpl.ACTION_PROGRESS_INFO, 0);
                LogUtil.d("progress", "BlockchainBroadcastReceiver" + progress);
                pbSync.setProgress(progress);
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
