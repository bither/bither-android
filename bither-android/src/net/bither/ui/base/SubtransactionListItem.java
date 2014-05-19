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

import java.math.BigInteger;

import net.bither.R;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SubtransactionListItem extends FrameLayout {
	private TextView tvAddress;
	private TextView tvBtc;

	public SubtransactionListItem(Context context) {
		super(context);
		removeAllViews();
		addView(LayoutInflater.from(context).inflate(
				R.layout.list_item_transaction_address, null),
				LayoutParams.MATCH_PARENT, UIUtil.dip2pix(40));
		tvAddress = (TextView) findViewById(R.id.tv_subtransaction_address);
		tvBtc = (TextView) findViewById(R.id.tv_subtransaction_btc);
	}

	private SubtransactionListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private SubtransactionListItem(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setTextColor(int color) {
		tvAddress.setTextColor(color);
		tvBtc.setTextColor(color);
	}

	public void setContent(String address, BigInteger value) {
		if (!StringUtil.compareString(address,
				getContext().getString(R.string.address_cannot_be_parsed))
				&& !StringUtil.compareString(address,
						getContext().getString(R.string.input_coinbase))) {
			tvAddress.setText(WalletUtils.formatHash(address, 4, 20));
		} else {
			tvAddress.setText(address);
		}
		if (value != null) {
			tvBtc.setText(GenericUtils.formatValue(value));
		} else {
			tvBtc.setText("");
		}
	}
}
