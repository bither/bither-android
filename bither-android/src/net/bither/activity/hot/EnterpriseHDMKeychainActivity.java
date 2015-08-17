/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.activity.hot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.adapter.hot.EnterpriseHDMKeychainAdapter;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.EnterpriseHDMAddress;
import net.bither.bitherj.core.EnterpriseHDMKeychain;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.AddressInfoChangedObserver;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.BroadcastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by songchenwen on 15/6/9.
 */
public class EnterpriseHDMKeychainActivity extends SwipeRightFragmentActivity {
    private static final int AddCode = 1123;
    private static final int AddAddressCode = 1739;

    private ArrayList<EnterpriseHDMAddress> addresses = new ArrayList<EnterpriseHDMAddress>();

    private ListView lv;
    private EnterpriseHDMKeychainAdapter adapter;
    private EnterpriseHDMKeychain keychain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise_hdm_keychain);
        initView();
        keychain = AddressManager.getInstance().getEnterpriseHDMKeychain();
        if (keychain == null) {
            startActivityForResult(new Intent(this, AddEnterpriseHDMKeychainActivity.class),
                    AddCode);
            return;
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        lv = (ListView) findViewById(R.id.lv);
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        adapter = new EnterpriseHDMKeychainAdapter(this, addresses);
        lv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    private void load() {
        if (AbstractApp.addressIsReady) {
            if (keychain != null) {
                addresses.clear();
                addresses.addAll(keychain.getAddresses());
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastUtil.ACTION_MARKET);
        filter.addAction(NotificationAndroidImpl.ACTION_SYNC_BLOCK_AND_WALLET_STATE);
        filter.addAction(NotificationAndroidImpl.ACTION_ADDRESS_LOAD_COMPLETE_STATE);
        filter.addAction(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE);
        filter.addAction(NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE);
        registerReceiver(broadcastReceiver, filter);
    }

    private void unregisterReceiver() {
        unregisterReceiver(broadcastReceiver);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String a = null;

            if (intent.hasExtra(BroadcastUtil.ACTION_ADDRESS_ERROR)) {
                int errorCode = intent.getExtras().getInt(BroadcastUtil.ACTION_ADDRESS_ERROR);

                if (HandlerMessage.MSG_ADDRESS_NOT_MONITOR == errorCode) {
                    int id = R.string.address_monitor_failed_multiple_address;
                    DropdownMessage.showDropdownMessage(EnterpriseHDMKeychainActivity.this, id);
                    load();
                }
            }
            if (intent.hasExtra(NotificationAndroidImpl.MESSAGE_ADDRESS)) {
                a = intent.getStringExtra(NotificationAndroidImpl.MESSAGE_ADDRESS);
            }
            int itemCount = lv.getChildCount();
            for (int i = 0;
                 i < itemCount;
                 i++) {
                View v = lv.getChildAt(i);
                if (v instanceof AddressInfoChangedObserver) {
                    AddressInfoChangedObserver o = (AddressInfoChangedObserver) v;
                    o.onAddressInfoChanged(a);
                }
                if (v instanceof MarketTickerChangedObserver) {
                    MarketTickerChangedObserver o = (MarketTickerChangedObserver) v;
                    o.onMarketTickerChanged();
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AddCode || requestCode == AddAddressCode) {
            if (resultCode != RESULT_OK && requestCode == AddCode) {
                finish();
                return;
            }
            keychain = AddressManager.getInstance().getEnterpriseHDMKeychain();
            if (keychain == null) {
                finish();
                return;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private View.OnClickListener optionClick = new DialogWithActions
            .DialogWithActionsClickListener() {

        @Override
        protected List<DialogWithActions.Action> getActions() {
            return Arrays.asList(new DialogWithActions.Action[]{new DialogWithActions.Action(R
                    .string.enterprise_hdm_keychain_add_new_address, new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(new Intent(EnterpriseHDMKeychainActivity.this,
                            EnterpriseHDMKeychainAddNewAddressActivity.class), AddAddressCode);
                    overridePendingTransition(R.anim.slide_in_right, 0);
                }
            })});
        }
    };
}
