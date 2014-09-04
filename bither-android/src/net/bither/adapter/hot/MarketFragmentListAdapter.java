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

package net.bither.adapter.hot;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.bither.model.Market;
import net.bither.ui.base.MarketFragmentListItemView;

import java.util.List;

public class MarketFragmentListAdapter extends BaseAdapter {
	private FragmentActivity activity;
	private List<Market> markets;

	public MarketFragmentListAdapter(FragmentActivity activity,
			List<Market> markets) {
		this.activity = activity;
		this.markets = markets;
	}

	@Override
	public int getCount() {
		if (markets == null) {
			return 0;
		}
		return markets.size();
	}

	@Override
	public Market getItem(int position) {
		return markets.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		MarketFragmentListItemView view;
		if (convertView == null
				|| !(convertView instanceof MarketFragmentListItemView)) {
			convertView = new MarketFragmentListItemView(activity);
		}
		view = (MarketFragmentListItemView) convertView;
		view.setMarket(markets.get(position), position);
		return convertView;
	}
}
