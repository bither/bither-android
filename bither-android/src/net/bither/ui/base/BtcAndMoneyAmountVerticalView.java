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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.util.UnitUtilWrapper;

import java.math.BigInteger;

public class BtcAndMoneyAmountVerticalView extends FrameLayout {

	private TextView tvBtc;
	private BtcToMoneyTextView tvMoney;

	public BtcAndMoneyAmountVerticalView(Context context) {
		super(context);
		initView();
	}

	public BtcAndMoneyAmountVerticalView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public BtcAndMoneyAmountVerticalView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		addView(LayoutInflater.from(getContext()).inflate(
				R.layout.layout_vertical_btc_and_money_amount, null),
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tvBtc = (TextView) findViewById(R.id.tv_btc);
		tvMoney = (BtcToMoneyTextView) findViewById(R.id.tv_money);
	}

	public void setAmount(BigInteger btc) {
		if (btc == null) {
			tvBtc.setText(BitherSetting.UNKONW_ADDRESS_STRING);
			tvMoney.setText(BitherSetting.UNKONW_ADDRESS_STRING);
			return;
		}
		if (btc.compareTo(BigInteger.ZERO) >= 0) {
			tvBtc.setTextColor(getContext().getResources().getColor(
					R.color.green));
		} else {
			tvBtc.setTextColor(getContext().getResources()
					.getColor(R.color.red));
		}
		tvBtc.setText(UnitUtilWrapper.formatValue(btc.longValue()));
		tvMoney.setBigInteger(btc);
	}

	public void onResume() {
		tvMoney.onResume();

	}

	public void onPause() {
		tvMoney.onPause();
	}
}
