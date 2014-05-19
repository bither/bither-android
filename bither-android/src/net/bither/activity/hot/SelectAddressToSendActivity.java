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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.SendActivity;
import net.bither.model.BitherAddress;
import net.bither.ui.base.DialogAddressFull;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.uri.BitcoinURI;

public class SelectAddressToSendActivity extends SwipeRightActivity {
	public static final String INTENT_EXTRA_ADDRESS = "address";
	public static final String INTENT_EXTRA_AMOUNT = "amount";
	public static final String INTENT_EXTRA_TRANSACTION = "transaction";
	public static final int SEND_REQUEST_CODE = 437;

	private String receivingAddress;
	private BigInteger btc;
	private boolean isAppInternal = false;

	private TextView tvAddress;
	private TextView tvAmount;
	private ListView lv;
	private TextView tvNoAddress;
	private ProgressBar pb;

	private ArrayList<AddressBalance> addresses = new ArrayList<AddressBalance>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!processIntent()) {
			finish();
			return;
		}
		setContentView(R.layout.activity_select_address_to_send);
		initView();
		getAddressesForSending();
	}

	private void initView() {
		if (!isAppInternal) {
			mTouchView.addIgnoreView(mTouchView);
		}
		findViewById(R.id.btn_cancel).setOnClickListener(
				new BackClickListener());
		tvAddress = (TextView) findViewById(R.id.tv_address);
		tvAmount = (TextView) findViewById(R.id.tv_btc);
		tvNoAddress = (TextView) findViewById(R.id.tv_no_address);
		lv = (ListView) findViewById(R.id.lv);
		pb = (ProgressBar) findViewById(R.id.pb);
		lv.setAdapter(adapter);
		tvAddress.setText(receivingAddress);
		tvAmount.setText(GenericUtils.formatValueWithBold(btc));
	}

	private void getAddressesForSending() {
		pb.setVisibility(View.VISIBLE);
		lv.setVisibility(View.INVISIBLE);
		tvNoAddress.setVisibility(View.GONE);
		addresses.clear();
		new Thread() {
			public void run() {
				List<BitherAddress> as = WalletUtils.getBitherAddressList(true);
				ArrayList<AddressBalance> availableAddresses = new ArrayList<AddressBalance>();
				for (BitherAddress a : as) {
					BigInteger balance = a.getBalance(BalanceType.AVAILABLE);
					if (balance.compareTo(btc) >= 0) {
						availableAddresses.add(new AddressBalance(a, balance));
					}
				}
				addresses.addAll(availableAddresses);
				Collections.sort(addresses, Collections.reverseOrder());
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.notifyDataSetChanged();
						if (addresses.size() > 0) {
							lv.setVisibility(View.VISIBLE);
							tvNoAddress.setVisibility(View.GONE);
						} else {
							lv.setVisibility(View.GONE);
							tvNoAddress.setVisibility(View.VISIBLE);
						}
						pb.setVisibility(View.GONE);
					}
				});
			}
		}.start();
	}

	private BaseAdapter adapter = new BaseAdapter() {
		private LayoutInflater inflater;

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (inflater == null) {
				inflater = LayoutInflater
						.from(SelectAddressToSendActivity.this);
			}
			ViewHolder h;
			if (convertView != null) {
				h = (ViewHolder) convertView.getTag();
			} else {
				convertView = inflater.inflate(
						R.layout.list_item_select_address_to_send, null);
				h = new ViewHolder(convertView);
				convertView.setTag(h);
			}
			AddressBalance a = getItem(position);
			h.tvAddress.setText(a.address.getShortAddress());
			h.tvBalance.setText(GenericUtils.formatValueWithBold(a.balance));
			if (a.address.hasPrivateKey()) {
				h.ivType.setImageResource(R.drawable.address_type_private);
			} else {
				h.ivType.setImageResource(R.drawable.address_type_watchonly);
			}
			h.ibtnAddressFull
					.setOnClickListener(new AddressFullClick(a.address));
			convertView.setOnClickListener(new ListItemClick(a.address));
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public AddressBalance getItem(int position) {
			return addresses.get(position);
		}

		@Override
		public int getCount() {
			return addresses.size();
		}

		class ViewHolder {
			TextView tvAddress;
			TextView tvBalance;
			ImageView ivType;
			ImageButton ibtnAddressFull;

			public ViewHolder(View v) {
				tvAddress = (TextView) v.findViewById(R.id.tv_address);
				tvBalance = (TextView) v.findViewById(R.id.tv_balance);
				ivType = (ImageView) v.findViewById(R.id.iv_type);
				ibtnAddressFull = (ImageButton) v
						.findViewById(R.id.ibtn_address_full);
			}
		}
	};

	private class ListItemClick implements OnClickListener {
		private BitherAddress address;

		public ListItemClick(BitherAddress address) {
			this.address = address;
		}

		@Override
		public void onClick(View v) {
			int position;
			Class<?> target;
			if (address.hasPrivateKey()) {
				position = WalletUtils.getPrivateAddressList().indexOf(address);
				target = SendActivity.class;
			} else {
				position = WalletUtils.getWatchOnlyAddressList().indexOf(
						address);
				target = GenerateUnsignedTxActivity.class;
			}
			Intent intent = new Intent(SelectAddressToSendActivity.this, target);
			intent.putExtra(
					BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG,
					position);
			intent.putExtra(INTENT_EXTRA_ADDRESS, receivingAddress);
			intent.putExtra(INTENT_EXTRA_AMOUNT, btc);
			intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			startActivity(intent);
			lv.postDelayed(finishRunnable, 800);
		}
	}

	private class AddressFullClick implements OnClickListener {
		private BitherAddress address;

		public AddressFullClick(BitherAddress address) {
			this.address = address;
		}

		@Override
		public void onClick(View v) {
			ArrayMap<String, BigInteger> map = new ArrayMap<String, BigInteger>();
			map.put(address.getAddress(), null);
			DialogAddressFull dialog = new DialogAddressFull(
					SelectAddressToSendActivity.this, map);
			dialog.show(v);
		}
	}

	private static final class AddressBalance implements
			Comparable<AddressBalance> {
		public BitherAddress address;
		public BigInteger balance;

		public AddressBalance(BitherAddress address, BigInteger balance) {
			this.address = address;
			this.balance = balance;
		}

		@Override
		public int compareTo(AddressBalance another) {
			if (address.hasPrivateKey() && !another.address.hasPrivateKey()) {
				return 1;
			}
			if (!address.hasPrivateKey() && another.address.hasPrivateKey()) {
				return -1;
			}
			return balance.compareTo(another.balance);
		}
	}

	public static void start(Context context, String address, BigInteger btc) {
		Intent intent = new Intent(context, SelectAddressToSendActivity.class);
		intent.putExtra(INTENT_EXTRA_ADDRESS, address);
		intent.putExtra(INTENT_EXTRA_AMOUNT, btc);
		if (context instanceof Activity) {
			Activity a = (Activity) context;
			a.startActivityForResult(intent, SEND_REQUEST_CODE);
		} else {
			context.startActivity(intent);
		}
	}

	private boolean processIntent() {
		Intent intent = getIntent();
		if (StringUtil.compareString(intent.getScheme(), "bitcoin")
				&& intent.getData() != null
				&& intent.getAction().equals(Intent.ACTION_VIEW)) {
			isAppInternal = false;
			try {
				BitcoinURI bitcoinUri = new BitcoinURI(null,
						intent.getDataString());
				Address address = bitcoinUri.getAddress();
				BigInteger amount = bitcoinUri.getAmount();
				if (address != null) {
					receivingAddress = address.toString();
					btc = amount == null ? BigInteger.ZERO : amount;
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			isAppInternal = true;
			if (intent.getExtras().containsKey(INTENT_EXTRA_ADDRESS)) {
				String address = intent.getExtras().getString(
						INTENT_EXTRA_ADDRESS);
				if (address != null && StringUtil.validBicoinAddress(address)) {
					receivingAddress = address;
					btc = (BigInteger) intent.getExtras().getSerializable(
							INTENT_EXTRA_AMOUNT);
					if (btc == null) {
						btc = BigInteger.ZERO;
					}
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		lv.removeCallbacks(finishRunnable);
	}

	private Runnable finishRunnable = new Runnable() {

		@Override
		public void run() {
			finish();
		}
	};
}
