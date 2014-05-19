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

package net.bither.adapter.cold;

import java.util.List;

import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.ui.base.ColdAddressFragmentListItemView;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AddressOfColdFragmentListAdapter extends BaseAdapter {
	private FragmentActivity activity;

	private List<BitherAddressWithPrivateKey> privates;

	public AddressOfColdFragmentListAdapter(FragmentActivity activity,
			List<BitherAddressWithPrivateKey> privates) {
		this.activity = activity;
		this.privates = privates;
	}

	@Override
	public int getCount() {

		return this.privates.size();
	}

	@Override
	public Object getItem(int position) {

		return this.privates.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ColdAddressFragmentListItemView view;
		if (convertView == null
				|| !(convertView instanceof ColdAddressFragmentListItemView)) {
			convertView = new ColdAddressFragmentListItemView(activity);
		}
		view = (ColdAddressFragmentListItemView) convertView;
		BitherAddress a;

		a = privates.get(position);

		view.showAddress(a);
		return convertView;
	}
}
