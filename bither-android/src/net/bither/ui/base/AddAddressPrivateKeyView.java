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

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.AbstractWheelTextAdapter;
import net.bither.BitherSetting;
import net.bither.BitherSetting.AppMode;
import net.bither.R;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DialogPassword.DialogPasswordListener;
import net.bither.util.LogUtil;
import net.bither.util.WalletUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class AddAddressPrivateKeyView extends FrameLayout implements
		DialogPasswordListener {
	private WheelView wvCount;
	private Button btnAdd;
	private AddPrivateKeyActivity activity;
	private ArrayList<BitherAddressWithPrivateKey> addresses = new ArrayList<BitherAddressWithPrivateKey>();

	private DialogProgress dp;

	public AddAddressPrivateKeyView(AddPrivateKeyActivity context) {
		super(context);
		this.activity = context;
		initView();
	}

	public AddAddressPrivateKeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public AddAddressPrivateKeyView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		addView(LayoutInflater.from(getContext()).inflate(
				R.layout.fragment_add_address_private_key, null),
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		wvCount = (WheelView) findViewById(R.id.wv_count);
		btnAdd = (Button) findViewById(R.id.btn_add);
		dp = new DialogProgress(activity, R.string.please_wait);
		dp.setCancelable(false);
		wvCount.setViewAdapter(countAdapter);
		wvCount.setCurrentItem(0);
		btnAdd.setOnClickListener(addClick);
	}

	private OnClickListener addClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			DialogPassword dialog = new DialogPassword(getContext(),
					AddAddressPrivateKeyView.this);
			dialog.show();
		}
	};

	@Override
	public void onPasswordEntered(final String password) {
		wvCount.setKeepScreenOn(true);
		ThreadNeedService thread = new ThreadNeedService(dp, activity) {

			@Override
			public void runWithService(BlockchainService service) {
				int count = wvCount.getCurrentItem() + 1;
				addresses.clear();
				while (addresses.size() < count) {
					BitherAddressWithPrivateKey address = new BitherAddressWithPrivateKey();
					if (!WalletUtils.getBitherAddressList().contains(address)) {
						address.encrypt(password);
						addresses.add(address);
						WalletUtils.addAddressWithPrivateKey(service, address);
						LogUtil.d("Generate", "Address: " + addresses.size());
					}
				}
				Collections.reverse(addresses);
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						wvCount.setKeepScreenOn(false);
						if (dp.isShowing()) {
							dp.dismiss();
						}
						activity.save();
					}
				});
			}
		};
		thread.start();
	}

	public ArrayList<BitherAddressWithPrivateKey> getAddresses() {
		return addresses;
	}

	private AbstractWheelTextAdapter countAdapter = new AbstractWheelTextAdapter(
			getContext()) {
		private int max = getMaxCount();

		@Override
		public int getItemsCount() {
			return max;
		}

		@Override
		protected CharSequence getItemText(int index) {
			return String.valueOf(index + 1);
		}

		private int getMaxCount() {
			int max = 0;
			if (AppSharedPreference.getInstance().getAppMode() == AppMode.COLD) {
				max = BitherSetting.WATCH_ONLY_ADDRESS_COUNT_LIMIT
						- WalletUtils.getBitherAddressList(true).size();
			} else {
				max = BitherSetting.PRIVATE_KEY_OF_HOT_COUNT_LIMIT
						- WalletUtils.getPrivateAddressList().size();
			}
			return max;
		}
	};

}
