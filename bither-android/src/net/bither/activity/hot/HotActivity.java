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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.adapter.hot.HotFragmentPagerAdapter;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.fragment.Unselectable;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.fragment.hot.MarketFragment;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.AddErrorMsgRunnable;
import net.bither.runnable.ThreadNeedService;
import net.bither.runnable.UploadAvatarRunnable;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DialogPassword;
import net.bither.ui.base.DialogPassword.DialogPasswordListener;
import net.bither.ui.base.DialogProgress;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SyncProgressView;
import net.bither.ui.base.TabButton;
import net.bither.util.BroadcastUtil;
import net.bither.util.LogUtil;
import net.bither.util.ServiceUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class HotActivity extends FragmentActivity {
    private TabButton tbtnMessage;
    private TabButton tbtnMain;
    private TabButton tbtnMe;
    private FrameLayout flAddAddress;
    private HotFragmentPagerAdapter mAdapter;
    private ViewPager mPager;
    private SyncProgressView pbSync;
    private DialogProgress dp;

    protected void onCreate(Bundle savedInstanceState) {
        BroadcastUtil.removeProgressState();
        initAppState();
        super.onCreate(savedInstanceState);
        BitherApplication.hotActivity = this;
        setContentView(R.layout.activity_hot);
        initView();
        registerReceiver(broadcastReceiver, new IntentFilter(BroadcastUtil
                .ACTION_SYNC_BLOCK_AND_WALLET_STATE));
        registerReceiver(totalBitcoinBroadcastReceiver, new IntentFilter(BroadcastUtil
                        .ACTION_TOTAL_BITCOIN_STATE));
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
                ServiceUtil.doMarkTimerTask(true);
            }
        }, 500);
        addNewPrivateKey();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(totalBitcoinBroadcastReceiver);
        super.onDestroy();
        BitherApplication.hotActivity = null;
        ServiceUtil.doMarkTimerTask(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ServiceUtil.doMarkTimerTask(false);
    }

    @Override
    protected void onResume() {
        ServiceUtil.doMarkTimerTask(true);
        super.onResume();
    }

    private void addNewPrivateKey() {
        final AppSharedPreference preference = AppSharedPreference.getInstance();
        if (!preference.hasPrivateKey()) {
            dp = new DialogProgress(HotActivity.this, R.string.please_wait);
            dp.setCancelable(false);
            DialogPassword dialogPassword = new DialogPassword(HotActivity.this,
                    new DialogPasswordListener() {

                @Override
                public void onPasswordEntered(final String password) {
                    ThreadNeedService thread = new ThreadNeedService(dp, HotActivity.this) {

                        @Override
                        public void runWithService(BlockchainService service) {
                            BitherAddressWithPrivateKey address = new BitherAddressWithPrivateKey();
                            if (!WalletUtils.getBitherAddressList().contains(address)) {
                                address.encrypt(password);
                                WalletUtils.addAddressWithPrivateKey(service, address);
                                preference.setHasPrivateKey(true);
                            }
                            HotActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    Fragment fragment = getFragmentAtIndex(1);
                                    if (fragment instanceof Refreshable) {
                                        ((Refreshable) fragment).doRefresh();
                                    }

                                }
                            });
                        }
                    };
                    thread.start();
                }
            }
            );
            dialogPassword.setCancelable(false);
            dialogPassword.show();
        }
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

        tbtnMain.setIconResource(R.drawable.tab_main, R.drawable.tab_main_checked);
        tbtnMain.setBigInteger(null);
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
                if (WalletUtils.getPrivateAddressList() != null && WalletUtils
                        .getPrivateAddressList().size() > 0) {
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
                } else {
                    DropdownMessage.showDropdownMessage(HotActivity.this, R.string.wallet_loading);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            Fragment f = getFragmentAtIndex(1);
            if (f != null && f instanceof HotAddressFragment) {
                @SuppressWarnings("unchecked") ArrayList<String> addresses = (ArrayList<String>)
                        data.getExtras().getSerializable(BitherSetting.INTENT_REF
                                .ADDRESS_POSITION_PASS_VALUE_TAG);
                HotAddressFragment af = (HotAddressFragment) f;
                af.showAddressesAdded(addresses);
            }
            if (f != null && f instanceof Refreshable) {
                Refreshable r = (Refreshable) f;
                r.doRefresh();
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
                AddErrorMsgRunnable addErrorMsgRunnable = new AddErrorMsgRunnable();
                addErrorMsgRunnable.run();
                UploadAvatarRunnable uploadAvatarRunnable = new UploadAvatarRunnable();
                uploadAvatarRunnable.run();

            }
        }).start();
    }

    public void showNewCount() {

    }

    private final BlockchainBroadcastReceiver broadcastReceiver = new BlockchainBroadcastReceiver();

    private final class BlockchainBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent != null && intent.hasExtra(BroadcastUtil.ACTION_PROGRESS_INFO)) {
                double progress = intent.getDoubleExtra(BroadcastUtil.ACTION_PROGRESS_INFO, 0);
                LogUtil.d("progress", "BlockchainBroadcastReceiver" + progress);
                pbSync.setProgress(progress);
            }
        }
    }

    private final TotalBitcoinBroadcastReceiver totalBitcoinBroadcastReceiver = new
            TotalBitcoinBroadcastReceiver();

    private final class TotalBitcoinBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra(BroadcastUtil.ACTION_PRIVATEKEY_TOTAL_BITCOIN)) {
                BigInteger btc = (BigInteger) intent.getSerializableExtra(BroadcastUtil
                        .ACTION_PRIVATEKEY_TOTAL_BITCOIN);
                if (!WalletUtils.hasAnyAddresses()) {
                    tbtnMain.setBigInteger(null);
                } else {
                    tbtnMain.setBigInteger(btc);
                }
            }
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

}
