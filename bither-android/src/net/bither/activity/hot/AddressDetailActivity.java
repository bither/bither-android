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
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import net.bither.BitherSetting;
import net.bither.NotificationAndroidImpl;
import net.bither.R;
import net.bither.adapter.TransactionListAdapter;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.ui.base.AddressDetailHeader;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.TransactionListItem;
import net.bither.ui.base.dialog.DialogAddressAlias;
import net.bither.ui.base.dialog.DialogAddressWatchOnlyOption;
import net.bither.ui.base.dialog.DialogAddressWithPrivateKeyOption;
import net.bither.ui.base.dialog.DialogHDMAddressOptions;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.BroadcastUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddressDetailActivity extends SwipeRightFragmentActivity implements
        DialogAddressAlias.DialogAddressAliasDelegate {
    private int page = 1;
    private boolean hasMore = true;
    private boolean isLoding = false;

    protected int addressPosition;
    protected Address address;
    private ArrayList<Tx> transactions = new ArrayList<Tx>();
    private ListView lv;
    private FrameLayout flTitleBar;
    private TransactionListAdapter mAdapter;
    private AddressDetailHeader header;
    private Button btnAddressAlias;
    private TxAndBlockBroadcastReceiver txAndBlockBroadcastReceiver = new TxAndBlockBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_address_detail);
        initAddress();
        if (address == null) {
            finish();
            return;
        }
        initView();
    }

    protected void initAddress() {
        if (getIntent().getExtras().containsKey(BitherSetting.INTENT_REF
                .ADDRESS_POSITION_PASS_VALUE_TAG)) {
            addressPosition = getIntent().getExtras().getInt(BitherSetting.INTENT_REF
                    .ADDRESS_POSITION_PASS_VALUE_TAG);
            boolean hasPrivateKey = getIntent().getExtras().getBoolean(BitherSetting.INTENT_REF
                    .ADDRESS_HAS_PRIVATE_KEY_PASS_VALUE_TAG, false);
            boolean isHDM = getIntent().getExtras().getBoolean(BitherSetting.INTENT_REF
                    .ADDRESS_IS_HDM_KEY_PASS_VALUE_TAG, false);
            if (isHDM) {
                if (addressPosition >= 0 && AddressManager.getInstance().hasHDMKeychain() &&
                        AddressManager.getInstance().getHdmKeychain().getAddresses().size() >
                                addressPosition) {
                    address = AddressManager.getInstance().getHdmKeychain().getAddresses().get
                            (addressPosition);
                }
            } else if (hasPrivateKey) {
                if (addressPosition >= 0 && AddressManager.getInstance().getPrivKeyAddresses() !=
                        null && addressPosition < AddressManager.getInstance()
                        .getPrivKeyAddresses().size()) {
                    address = AddressManager.getInstance().getPrivKeyAddresses().get
                            (addressPosition);
                }
            } else {
                if (addressPosition >= 0 && AddressManager.getInstance().getWatchOnlyAddresses()
                        != null && addressPosition < AddressManager.getInstance()
                        .getWatchOnlyAddresses().size()) {
                    address = AddressManager.getInstance().getWatchOnlyAddresses().get
                            (addressPosition);
                }
            }
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(
                new IBackClickListener(0, R.anim.slide_out_right));
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        lv = (ListView) findViewById(R.id.lv);
        flTitleBar = (FrameLayout) findViewById(R.id.fl_title_bar);
        btnAddressAlias = (Button) findViewById(R.id.btn_address_alias);
        flTitleBar.setOnClickListener(scrollToTopClick);
        btnAddressAlias.setOnClickListener(aliasClick);
        mAdapter = new TransactionListAdapter(this, transactions, address);
        header = new AddressDetailHeader(this);
        lv.addHeaderView(header, null, false);
        lv.setAdapter(mAdapter);
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int lastFirstVisibleItem;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (firstVisibleItem + visibleItemCount >= totalItemCount - 6
                        && hasMore && !isLoding
                        && lastFirstVisibleItem < firstVisibleItem) {
                    page++;
                    loadTx();
                }
                lastFirstVisibleItem = firstVisibleItem;

            }
        });
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter txAndBlockReceiver = new IntentFilter();
        txAndBlockReceiver.addAction(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE);
        txAndBlockReceiver.addAction(NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE);
        IntentFilter marketFilter = new IntentFilter(BroadcastUtil.ACTION_MARKET);
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
        if (requestCode == BitherSetting.INTENT_REF.SEND_REQUEST_CODE && resultCode == RESULT_OK) {
            DropdownMessage.showDropdownMessage(this, R.string.send_success);
            loadData();
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    public void loadData() {
        header.showAddress(address, addressPosition);
        onAddressAliasChanged(address, address.getAlias());
        page = 1;
        loadTx();
    }

    private void loadTx() {
        if (address != null && address.isSyncComplete() && !isLoding && hasMore) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isLoding = true;
                    final List<Tx> txs = address.getTxs(page);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (page == 1) {
                                transactions.clear();
                            }
                            if (txs != null && txs.size() > 0) {
                                transactions.addAll(txs);
                                hasMore = true;
                            } else {
                                hasMore = false;
                            }
                            Collections.sort(transactions);
                            mAdapter.notifyDataSetChanged();
                            isLoding = false;
                        }
                    });
                }
            }).start();
        }
    }


    private OnClickListener optionClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            optionClicked();
        }
    };

    protected void optionClicked() {
        Dialog dialog = null;
        if (address.isHDAccount()) {
            return;
        }
        if (address.isHDM()) {
            new DialogHDMAddressOptions(AddressDetailActivity.this, (HDMAddress) address, true)
                    .show();
        } else if (address.hasPrivKey()) {
            dialog = new DialogAddressWithPrivateKeyOption(AddressDetailActivity.this, address);
        } else {
            dialog = new DialogAddressWatchOnlyOption(AddressDetailActivity.this, address, new
                    Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
        if (dialog != null) {
            dialog.show();
        }
    }

    private OnClickListener scrollToTopClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (lv.getFirstVisiblePosition() != 0) {
                lv.post(new SmoothScrollListRunnable(lv, 0, null));
            }
        }
    };

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

    private OnClickListener aliasClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogAddressAlias(v.getContext(), address, AddressDetailActivity.this).show();
        }
    };

    @Override
    public void onAddressAliasChanged(Address address, String alias) {
        if (!Utils.isEmpty(alias)) {
            btnAddressAlias.setVisibility(View.VISIBLE);
            btnAddressAlias.setText(alias);
        } else {
            btnAddressAlias.setVisibility(View.GONE);
            btnAddressAlias.setText("");
        }
    }

    protected void notifyAddressBalanceChange(String address) {
        if (Utils.compareString(address, this.address.getAddress())) {
            loadData();
        }
    }

    private final class TxAndBlockBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || (!Utils.compareString(NotificationAndroidImpl
                    .ACTION_ADDRESS_BALANCE, intent.getAction()) && !Utils.compareString
                    (NotificationAndroidImpl.ACTION_SYNC_LAST_BLOCK_CHANGE, intent.getAction()))) {
                return;
            }
            if (intent.hasExtra(NotificationAndroidImpl.ACTION_ADDRESS_BALANCE)) {
                String receiveAddressStr = intent.getStringExtra(NotificationAndroidImpl
                        .MESSAGE_ADDRESS);
                notifyAddressBalanceChange(receiveAddressStr);
            } else {
                loadData();
            }

        }
    }

}
