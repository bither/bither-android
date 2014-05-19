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

import net.bither.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;

public class PinnedHeaderAddressExpandableListView extends
		PinnedHeaderExpandableListView {

	private LayoutInflater inflater;
	private View headerView;

	public PinnedHeaderAddressExpandableListView(Context context) {
		super(context);
		initHeaderView(context);
	}

	public PinnedHeaderAddressExpandableListView(Context context,
			AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initHeaderView(context);
	}

	public PinnedHeaderAddressExpandableListView(Context context,
			AttributeSet attrs) {
		super(context, attrs);
		initHeaderView(context);
	}

	private void initHeaderView(Context context) {
		inflater = LayoutInflater.from(context);
		headerView = inflater.inflate(R.layout.list_item_address_group, null);
		AbsListView.LayoutParams lp = new LayoutParams(
				AbsListView.LayoutParams.MATCH_PARENT,
				AbsListView.LayoutParams.WRAP_CONTENT);
		headerView.setLayoutParams(lp);
		this.setPinnedHeaderView(headerView);
	}

}
