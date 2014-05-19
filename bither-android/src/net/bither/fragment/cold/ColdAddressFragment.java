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

package net.bither.fragment.cold;

import java.util.ArrayList;
import java.util.List;

import net.bither.R;
import net.bither.adapter.cold.AddressOfColdFragmentListAdapter;
import net.bither.fragment.Refreshable;
import net.bither.fragment.Selectable;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.ui.base.SmoothScrollListRunnable;
import net.bither.util.BroadcastUtil;
import net.bither.util.WalletUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

public class ColdAddressFragment extends Fragment implements Refreshable,
		Selectable {
	private ListView lvPrivate;
	private View ivNoAddress;
	private AddressOfColdFragmentListAdapter mAdapter;
	private List<BitherAddressWithPrivateKey> privates;
	private boolean isWalletReady = false;

	private List<String> addressesToShowAdded;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getActivity().registerReceiver(
				walletReadyReceiver,
				new IntentFilter(
						BroadcastUtil.ACTION_ADDRESS_LOAD_COMPLETE_STATE));
		View view = inflater.inflate(R.layout.fragment_cold_address, container,
				false);
		lvPrivate = (ListView) view.findViewById(R.id.lv_address);
		ivNoAddress = view.findViewById(R.id.iv_no_address);
		privates = new ArrayList<BitherAddressWithPrivateKey>();
		mAdapter = new AddressOfColdFragmentListAdapter(getActivity(), privates);
		lvPrivate.setAdapter(mAdapter);
		List<BitherAddressWithPrivateKey> ps = WalletUtils
				.getPrivateAddressList();
		if (ps != null) {
			privates.addAll(ps);
			mAdapter.notifyDataSetChanged();
		}
		return view;
	}

	@Override
	public void onDestroyView() {
		getActivity().unregisterReceiver(walletReadyReceiver);
		super.onDestroyView();
	}

	private BroadcastReceiver walletReadyReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			isWalletReady = true;
			refresh();
		}
	};

	public void refresh() {
		if (isWalletReady) {
			List<BitherAddressWithPrivateKey> ps = WalletUtils
					.getPrivateAddressList();
			if (ps != null) {
				privates.clear();
				privates.addAll(ps);
				mAdapter.notifyDataSetChanged();
			}
			if (privates.size() == 0) {
				ivNoAddress.setVisibility(View.VISIBLE);
				lvPrivate.setVisibility(View.GONE);
			} else {
				ivNoAddress.setVisibility(View.GONE);
				lvPrivate.setVisibility(View.VISIBLE);
			}
			if (addressesToShowAdded != null) {
				lvPrivate.postDelayed(showAddressesAddedRunnable, 600);
			}
		}
	}

	public void showAddressesAdded(List<String> addresses) {
		addressesToShowAdded = addresses;
	}

	@Override
	public void onSelected() {

	}

	@Override
	public void doRefresh() {
		if (lvPrivate == null) {
			return;
		}
		if (lvPrivate.getFirstVisiblePosition() != 0) {
			lvPrivate.post(new SmoothScrollListRunnable(lvPrivate, 0,
					new Runnable() {
						@Override
						public void run() {
							refresh();
						}
					}));
		} else {
			refresh();
		}
	}

	private Runnable showAddressesAddedRunnable = new Runnable() {
		@Override
		public void run() {
			if (addressesToShowAdded == null
					|| addressesToShowAdded.size() == 0) {
				return;
			}
			for (int i = 0; i < addressesToShowAdded.size()
					&& i < lvPrivate.getChildCount(); i++) {
				View v = lvPrivate.getChildAt(i);
				v.startAnimation(AnimationUtils.loadAnimation(getActivity(),
						R.anim.address_notification));
			}
			addressesToShowAdded = null;
		}
	};
}
