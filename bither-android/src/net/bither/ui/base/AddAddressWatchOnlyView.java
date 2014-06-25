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

package net.bither.ui.base;

import java.util.ArrayList;
import java.util.Collections;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.ScanQRCodeTransportActivity;
import net.bither.model.BitherAddress;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class AddAddressWatchOnlyView extends FrameLayout {

	private AddPrivateKeyActivity activity;
	private Button btnScan;
	private DialogProgress dp;
	private ArrayList<String> addresses = new ArrayList<String>();
	private Fragment fragment;

	public AddAddressWatchOnlyView(AddPrivateKeyActivity context,
			Fragment fragment) {
		super(context);
		this.activity = context;
		this.fragment = fragment;
		initView();
	}

	public AddAddressWatchOnlyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public AddAddressWatchOnlyView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		View v = LayoutInflater.from(getContext()).inflate(
				R.layout.fragment_add_address_watch_only, null);
		addView(v, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		btnScan = (Button) findViewById(R.id.btn_scan);
		btnScan.setOnClickListener(scanClick);
		dp = new DialogProgress(getContext(), R.string.please_wait);
		dp.setCancelable(false);
	}

	private OnClickListener scanClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(activity,
					ScanQRCodeTransportActivity.class);
			intent.putExtra(
					BitherSetting.INTENT_REF.TITLE_STRING,
					getContext()
							.getString(
									R.string.scan_for_all_addresses_in_bither_cold_title));
			if (fragment != null) {
				fragment.startActivityForResult(
						intent,
						BitherSetting.INTENT_REF.SCAN_ALL_IN_BITHER_COLD_REUEST_CODE);

			} else {
				activity.startActivityForResult(
						intent,
						BitherSetting.INTENT_REF.SCAN_ALL_IN_BITHER_COLD_REUEST_CODE);
			}
		}
	};

	public ArrayList<String> getAddresses() {
		return addresses;
	}

	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BitherSetting.INTENT_REF.SCAN_ALL_IN_BITHER_COLD_REUEST_CODE
				&& resultCode == Activity.RESULT_OK) {
			if (data.getExtras().containsKey(ScanActivity.INTENT_EXTRA_RESULT)) {
				final String content = data
						.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
				if (StringUtil.isEmpty(content) && !checkQrCodeContent(content)) {
					DropdownMessage
							.showDropdownMessage(
									activity,
									R.string.scan_for_all_addresses_in_bither_cold_failed);
					return true;
				}
				ThreadNeedService thread = new ThreadNeedService(dp, activity) {

					@Override
					public void runWithService(BlockchainService service) {
						processQrCodeContent(content, service);

					}
				};
				thread.start();
			}
			return true;
		}
		return false;
	}

	private boolean checkQrCodeContent(String content) {
		String[] strs = content.split(StringUtil.QR_CODE_SPLIT);
		for (String str : strs) {
			if (str.length() != 66) {
				return false;
			}
		}
		return true;
	}

	private void processQrCodeContent(String content, BlockchainService service) {
		String[] strs = content.split(StringUtil.QR_CODE_SPLIT);
		ArrayList<BitherAddress> wallets = new ArrayList<BitherAddress>();
		addresses.clear();
		try {
			for (String str : strs) {
				byte[] pub = StringUtil.hexStringToByteArray(str);
				BitherAddress address = new BitherAddress(pub);
				wallets.add(address);
			}
			Collections.reverse(wallets);
            WalletUtils.addBitherAddress(service, wallets);
			for (BitherAddress address : wallets) {
				if (!WalletUtils.getBitherAddressList().contains(address)) {
					addresses.add(address.getAddress());
				}
			}

			Collections.reverse(addresses);
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (dp.isShowing()) {
						dp.setThread(null);
						dp.dismiss();
					}
					activity.save();
				}
			});
		} catch (Exception e) {
			if (dp.isShowing()) {
				dp.setThread(null);
				dp.dismiss();
			}
			DropdownMessage.showDropdownMessage(activity,
					R.string.scan_for_all_addresses_in_bither_cold_failed);

		}
	}
}
