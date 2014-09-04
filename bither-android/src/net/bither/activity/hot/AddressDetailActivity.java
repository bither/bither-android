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

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.adapter.TransactionListAdapter;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.NotificationUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.AddressDetailHeader;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.TransactionListItem;
import net.bither.ui.base.dialog.DialogAddressWatchOnlyOption;
import net.bither.ui.base.dialog.DialogAddressWithPrivateKeyOption;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.BroadcastUtil;

import java.util.ArrayList;
import java.util.List;

public class AddressDetailActivity extends SwipeRightFragmentActivity {
    private int addressPosition;
    private boolean hasPrivateKey;
    private Address address;
    private OnClickListener optionClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Dialog dialog = null;
            if (address.hasPrivKey()) {
                dialog = new DialogAddressWithPrivateKeyOption(
                        AddressDetailActivity.this, address);
            } else {
                dialog = new DialogAddressWatchOnlyOption(
                        AddressDetailActivity.this, address, new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }
                );
            }
            dialog.show();
        }
    };
    private ArrayList<Tx> transactions = new ArrayList<Tx>();
    private ListView lv;
    private OnClickListener scrollToTopClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (lv.getFirstVisiblePosition() != 0) {
                lv.post(new SmoothScrollListRunnable(lv, 0, null));
            }
        }
    };
    private FrameLayout flTitleBar;
    private TransactionListAdapter mAdapter;
    private AddressDetailHeader header;
    private TxAndBlockBroadcastReceiver txAndBlockBroadcastReceiver = new TxAndBlockBroadcastReceiver();
    private BroadcastReceiver marketBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int itemCount = lv.getChildCount();
            for (int i = 0;
                 i < itemCount;
                 i++) {
                View v = lv.getChildAt(i);
                if (v instanceof MarketTickerChangedObserver) {
                    MarketTickerChangedObserver o = (MarketTickerChangedObserver) v;
                    o.onMarketTickerChanged();
                }
            }

        }
    };


    private final class TxAndBlockBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    (!Utils.compareString(NotificationUtil.ACTION_ADDRESS_BALANCE, intent.getAction())
                            && !Utils.compareString(NotificationUtil.ACTION_SYNC_LAST_BLOCK_CHANGE, intent.getAction()))) {
                return;
            }
            if (intent.hasExtra(NotificationUtil.ACTION_ADDRESS_BALANCE)) {
                String receiveAddressStr = intent.getStringExtra(NotificationUtil.MESSAGE_ADDRESS);
                if (Utils.compareString(receiveAddressStr, address.getAddress())) {
                    loadData();
                }

            } else {
                loadData();
            }

        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter txAndBlockReceiver = new IntentFilter();
        txAndBlockReceiver.addAction(NotificationUtil.ACTION_ADDRESS_BALANCE);
        txAndBlockReceiver.addAction(NotificationUtil.ACTION_SYNC_LAST_BLOCK_CHANGE);
        IntentFilter marketFilter = new IntentFilter(
                BroadcastUtil.ACTION_MARKET);
        registerReceiver(txAndBlockBroadcastReceiver, txAndBlockReceiver);
        registerReceiver(marketBroadcastReceiver, marketFilter);
        for (int i = 0;
             i < lv.getChildCount();
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof TransactionListItem) {
                TransactionListItem item = (TransactionListItem) v;
                item.onResume();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SEND_REQUEST_CODE
                && resultCode == RESULT_OK) {
            DropdownMessage.showDropdownMessage(this, R.string.send_success);
            loadData();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_address_detail);
        if (getIntent().getExtras().containsKey(
                BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG)) {
            addressPosition = getIntent().getExtras().getInt(
                    BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG);
            hasPrivateKey = getIntent()
                    .getExtras()
                    .getBoolean(
                            BitherSetting.INTENT_REF.ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG,
                            false);
            if (hasPrivateKey) {
                if (addressPosition >= 0
                        && AddressManager.getInstance().getPrivKeyAddresses() != null && addressPosition <
                        AddressManager.getInstance().getPrivKeyAddresses().size()) {
                    address = AddressManager.getInstance().getPrivKeyAddresses().get(
                            addressPosition);
                }
            } else {
                if (addressPosition >= 0
                        && AddressManager.getInstance().getWatchOnlyAddresses() != null && addressPosition <
                        AddressManager.getInstance().getWatchOnlyAddresses().size()) {
                    address = AddressManager.getInstance().getWatchOnlyAddresses().get(
                            addressPosition);
                }
            }
        }
        if (address == null) {
            finish();
        }
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(
                new BackClickListener(0, R.anim.slide_out_right));
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        lv = (ListView) findViewById(R.id.lv);
        flTitleBar = (FrameLayout) findViewById(R.id.fl_title_bar);
        flTitleBar.setOnClickListener(scrollToTopClick);
        mAdapter = new TransactionListAdapter(this, transactions, address);
        header = new AddressDetailHeader(this);
        lv.addHeaderView(header, null, false);
        lv.setAdapter(mAdapter);
        loadData();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(txAndBlockBroadcastReceiver);
        unregisterReceiver(marketBroadcastReceiver);
        for (int i = 0;
             i < lv.getChildCount();
             i++) {
            View v = lv.getChildAt(i);
            if (v instanceof TransactionListItem) {
                TransactionListItem item = (TransactionListItem) v;
                item.onPause();
            }
        }
        super.onPause();
    }

    private void loadData() {
        header.showAddress(address, addressPosition);
        if (address != null && address.isSyncComplete()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<Tx> txs = address.getTxs();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transactions.clear();
                            transactions.addAll(txs);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
    }
}
