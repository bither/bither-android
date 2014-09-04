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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;
import net.bither.model.Check;
import net.bither.util.CheckUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;

public class ColdWalletInitCheckView extends LinearLayout {
	public static final int CheckAnimDuration = 1200;

	public ColdWalletInitCheckView(Context context) {
		super(context);
		initView();
	}

	public ColdWalletInitCheckView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public ColdWalletInitCheckView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		setOrientation(VERTICAL);
		ArrayList<Check> checks = new ArrayList<Check>();
		checks.add(CheckUtil.initCheckOfWifi());
		checks.add(CheckUtil.initCheckOf3G());
		checks.add(CheckUtil.initCheckOfBluetooth());
		for (int i = 0; i < checks.size(); i++) {
			addView(new ColdWalletInitCheckItemView(checks.get(i)),
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			if (i < checks.size() - 1) {
				View v = new View(getContext());
				v.setBackgroundResource(R.color.list_cell_divider);
				addView(v, LayoutParams.MATCH_PARENT, 1);
			}
		}
		check();
	}

	public boolean check() {
		boolean result = true;
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v instanceof ColdWalletInitCheckItemView) {
				ColdWalletInitCheckItemView item = (ColdWalletInitCheckItemView) v;
				if (!item.check()) {
					result = false;
				}
			}
		}
		return result;
	}

	public boolean checkAnim() {
		boolean result = true;
		final ArrayList<ColdWalletInitCheckItemView> items = new ArrayList<ColdWalletInitCheckView.ColdWalletInitCheckItemView>();
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v instanceof ColdWalletInitCheckItemView) {
				items.add((ColdWalletInitCheckItemView) v);
			}
		}
		for (int i = 0; i < items.size(); i++) {
			final ColdWalletInitCheckItemView item = items.get(i);
			if (!item.check()) {
				result = false;
			}
			item.prepareAnim();
			if (i == 0) {
				item.pb.setVisibility(View.VISIBLE);
			}
			final int nextIndex = i + 1;
			item.postDelayed(new Runnable() {

				@Override
				public void run() {
					item.check();
					if (nextIndex < items.size()) {
						items.get(nextIndex).pb.setVisibility(View.VISIBLE);
					}
				}
			}, CheckAnimDuration / 3 * (i + 1));
		}
		return result;
	}

	public void prepareAnim() {
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v instanceof ColdWalletInitCheckItemView) {
				ColdWalletInitCheckItemView item = (ColdWalletInitCheckItemView) v;
				item.prepareAnim();
			}
		}
	}

	private class ColdWalletInitCheckItemView extends LinearLayout {
		private Check check;
		private TextView tv;
		private ImageView ivState;
		private ProgressBar pb;
		private Button btn;

		public ColdWalletInitCheckItemView(Check check) {
			super(ColdWalletInitCheckView.this.getContext());
			setOrientation(HORIZONTAL);
			setPadding(0, UIUtil.dip2pix(5), 0, UIUtil.dip2pix(5));
			this.check = check;
			LayoutInflater.from(getContext()).inflate(
					R.layout.list_item_cold_wallet_init_check, this);
			tv = (TextView) findViewById(R.id.tv_check_title);
			ivState = (ImageView) findViewById(R.id.iv_check_state);
			pb = (ProgressBar) findViewById(R.id.pb);
			btn = (Button) findViewById(R.id.btn);
			btn.setOnClickListener(click);
			tv.setText(check.getTitle());
		}

		private OnClickListener click = new OnClickListener() {

			@Override
			public void onClick(View v) {
				check.operate();
			}
		};

		public void prepareAnim() {
			pb.setVisibility(View.GONE);
			btn.setVisibility(View.GONE);
			ivState.setVisibility(View.GONE);
			tv.setText(check.getTitleChecking());
		}

		public boolean check() {
			boolean result = check.check();
			pb.setVisibility(View.GONE);
			if (result) {
				tv.setText(check.getTitle());
				ivState.setVisibility(View.VISIBLE);
				btn.setVisibility(View.GONE);
			} else {
				tv.setText(check.getTitleFailed());
				ivState.setVisibility(View.GONE);
				btn.setVisibility(View.VISIBLE);
			}
			return result;
		}

	}

}
