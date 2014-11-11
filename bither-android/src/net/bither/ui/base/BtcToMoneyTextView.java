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
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.util.MarketUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;

import java.math.BigInteger;

public class BtcToMoneyTextView extends TextView implements
		MarketTickerChangedObserver {
	private double price = 0;
	private BigInteger btc;

	public BtcToMoneyTextView(Context context) {
		super(context);
		initView();
	}

	public BtcToMoneyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public BtcToMoneyTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		setCompoundDrawablePadding(UIUtil.dip2pix(3));
		getPrice();
	}

	private void getPrice() {
		Ticker ticker = MarketUtil.getTickerOfDefaultMarket();
		if (ticker != null) {
			price = ticker.getDefaultExchangePrice();
		}
	}

	public void onPause() {

	}

	public void onResume() {
		refreshText();
	}

	public void setBigInteger(BigInteger btc) {
		this.btc = btc;
		getPrice();
		if (btc != null) {
			if (price == 0) {
				setText(BitherSetting.UNKONW_ADDRESS_STRING);
			} else {
				double money = btc.doubleValue() / 100000000.0 * price;
				setText(AppSharedPreference.getInstance()
						.getDefaultExchangeType().getSymbol()
						+ Utils.formatDoubleToMoneyString(money));
			}
		} else {
			setText(BitherSetting.UNKONW_ADDRESS_STRING);
		}
	}

	private void refreshText() {
		getPrice();
		if (btc != null) {
			if (price == 0) {
				setText(BitherSetting.UNKONW_ADDRESS_STRING);
			} else {
				double money = btc.doubleValue() / 100000000.0 * price;
				setText(AppSharedPreference.getInstance()
						.getDefaultExchangeType().getSymbol()
						+ Utils.formatDoubleToMoneyString(money));
			}
		} else {
			setText(BitherSetting.UNKONW_ADDRESS_STRING);
		}
	}

	@Override
	public void onMarketTickerChanged() {
		refreshText();
	}
}
