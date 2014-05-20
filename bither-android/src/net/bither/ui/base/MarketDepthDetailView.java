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
import net.bither.preference.AppSharedPreference;
import net.bither.util.AnimationUtil;
import net.bither.util.CurrencySymbolUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MarketDepthDetailView extends LinearLayout {

	private TextView tvOrder;
	private TextView tvPrice;
	private TextView tvVolume;
	private TextView tvSymbol;

	private ImageView ivSymbolBtc;
	private Bitmap btcBit;

	public MarketDepthDetailView(Context context) {
		super(context);
		initView();
	}

	public MarketDepthDetailView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();

	}

	public MarketDepthDetailView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		addView(LayoutInflater.from(getContext()).inflate(
				R.layout.market_depth_detail_view, null),
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		this.tvOrder = (TextView) findViewById(R.id.tv_detail_order);
		this.tvPrice = (TextView) findViewById(R.id.tv_detail_price);
		this.tvVolume = (TextView) findViewById(R.id.tv_detail_volume);
		this.ivSymbolBtc = (ImageView) findViewById(R.id.iv_symbol_btc);
		this.btcBit = CurrencySymbolUtil.getBtcSlimSymbol(this.tvVolume);
		tvSymbol = (TextView) findViewById(R.id.tv_symbol);

	}

	public void setContent(String order, String price, String volume) {
		this.tvOrder.setText(order);
		this.tvPrice.setText(price);
		this.tvVolume.setText(" " + volume);
		this.ivSymbolBtc.setImageBitmap(btcBit);
		this.tvSymbol.setText(AppSharedPreference.getInstance()
				.getDefaultExchangeRate().getSymbol());
	}

	public void hide() {
		AnimationUtil.fadeIn(MarketDepthDetailView.this);

	}

	public void notifyViewMove(int x, int y, int parentWidth) {
		clearAnimation();
		if (getVisibility() != View.VISIBLE) {
			setVisibility(View.VISIBLE);
			if (x > parentWidth / 2) {
				moveViewDelayed(x, y, parentWidth);
			} else {
				moveView(x, y, parentWidth);
			}

		} else {
			moveView(x, y, parentWidth);
		}
	}

	private void moveViewDelayed(final int x, final int y, final int parentWidth) {
		if (getWidth() != 0) {
			moveView(x, y, parentWidth);
		} else {
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					moveViewDelayed(x, y, parentWidth);
				}
			}, 30);
		}
	}

	private void moveView(int x, int y, int parentWidth) {
		int leftMargin = 0;
		if (x > parentWidth / 2) {
			leftMargin = x - getWidth();
		} else {
			leftMargin = x;
		}
		AnimationUtil.moveMarginAnimation(MarketDepthDetailView.this,
				leftMargin, y);
	}

}
