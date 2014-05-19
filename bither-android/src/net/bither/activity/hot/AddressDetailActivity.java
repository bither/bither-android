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

import java.util.ArrayList;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.adapter.TransactionListAdapter;
import net.bither.model.BitherAddress;
import net.bither.ui.base.AddressDetailHeader;
import net.bither.ui.base.DialogAddressWatchOnlyOption;
import net.bither.ui.base.DialogAddressWithPrivateKeyOption;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.MarketTickerChangedObserver;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.BroadcastUtil;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
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

import com.google.bitcoin.core.Transaction;

public class AddressDetailActivity extends SwipeRightFragmentActivity {
	private int addressPosition;
	private boolean hasPrivateKey;
	private BitherAddress address;
	private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	private ListView lv;
	private FrameLayout flTitleBar;
	private TransactionListAdapter mAdapter;
	private AddressDetailHeader header;

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
						&& addressPosition < WalletUtils
								.getPrivateAddressList().size()) {
					address = WalletUtils.getPrivateAddressList().get(
							addressPosition);
				}
			} else {
				if (addressPosition >= 0
						&& addressPosition < WalletUtils
								.getWatchOnlyAddressList().size()) {
					address = WalletUtils.getWatchOnlyAddressList().get(
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

	private void loadData() {
		header.showAddress(address, addressPosition);
		if (address.getAddressInfo() != null && address.isReadyToShow()
				&& !address.isError()) {
			new Thread() {
				public void run() {
					final List<Transaction> txs = address
							.getTransactionsByTime();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							transactions.clear();
							transactions.addAll(txs);
							mAdapter.notifyDataSetChanged();
						}
					});
				};
			}.start();
		}
	}

	private OnClickListener optionClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Dialog dialog = null;
			if (address.hasPrivateKey()) {
				dialog = new DialogAddressWithPrivateKeyOption(
						AddressDetailActivity.this, address);
			} else {
				dialog = new DialogAddressWatchOnlyOption(
						AddressDetailActivity.this, address, new Runnable() {
							@Override
							public void run() {
								finish();
							}
						});
			}
			dialog.show();
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter addressFilter = new IntentFilter(
				BroadcastUtil.ACTION_ADDRESS_STATE);
		IntentFilter marketFilter = new IntentFilter(
				BroadcastUtil.ACTION_MARKET);
		registerReceiver(addressBroadcastReceiver, addressFilter);
		registerReceiver(marketBroadcastReceiver, marketFilter);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(addressBroadcastReceiver);
		unregisterReceiver(marketBroadcastReceiver);
		super.onPause();
	}

	private BroadcastReceiver marketBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int itemCount = lv.getChildCount();
			for (int i = 0; i < itemCount; i++) {
				View v = lv.getChildAt(i);
				if (v instanceof MarketTickerChangedObserver) {
					MarketTickerChangedObserver o = (MarketTickerChangedObserver) v;
					o.onMarketTickerChanged();
				}
			}

		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BitherSetting.INTENT_REF.SEND_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			DropdownMessage.showDropdownMessage(this, R.string.send_success);
			loadData();
		}
		super.onActivityResult(requestCode, resultCode, data);
	};

	private BroadcastReceiver addressBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String addressStr = null;
			if (intent.hasExtra(BroadcastUtil.ACTION_ADDRESS_STATE)) {
				addressStr = intent.getExtras().getString(
						BroadcastUtil.ACTION_ADDRESS_STATE);
			}
			if (StringUtil.compareString(addressStr, address.getAddress())) {
				loadData();
			}
		}
	};

	private OnClickListener scrollToTopClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (lv.getFirstVisiblePosition() != 0) {
				lv.post(new SmoothScrollListRunnable(lv, 0, null));
			}
		}
	};
}
