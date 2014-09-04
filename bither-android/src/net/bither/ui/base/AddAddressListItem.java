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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.util.StringUtil;

public class AddAddressListItem extends FrameLayout {

	public static interface AddressChangeListener {
		public void onAddressChanged();
	}

	private Activity activity;

	private EditText etAddress;
	private ImageButton ibtnScan;
	private ImageButton ibtnDelete;

	private AddressChangeListener listener;

	public AddAddressListItem(Activity context, AddressChangeListener listener) {
		super(context);
		this.listener = listener;
		this.activity = context;
		initView();
	}

	private AddAddressListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	private AddAddressListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		removeAllViews();
		addView(LayoutInflater.from(getContext()).inflate(
				R.layout.list_item_add_address, null),
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		etAddress = (EditText) findViewById(R.id.et_address);
		ibtnScan = (ImageButton) findViewById(R.id.ibtn_scan);
		ibtnDelete = (ImageButton) findViewById(R.id.ibtn_delete);
		ibtnScan.setOnClickListener(scanClick);
		ibtnDelete.setOnClickListener(deleteClick);
		etAddress.addTextChangedListener(textWatcher);
	}

	private TextWatcher textWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			checkAddress();
		}
	};

	private OnClickListener scanClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(activity, ScanActivity.class);
			intent.putExtra(BitherSetting.INTENT_REF.SCAN_ADDRESS_POSITION_TAG,
					getPositionInParent());
			activity.startActivityForResult(intent,
					BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
		}
	};

	public void setAddress(String address) {
		etAddress.setText(address);
		checkAddress();
	}

	public String getAddress() {
		if (etAddress.isEnabled()) {
			return null;
		} else {
			return etAddress.getText().toString();
		}
	}

	private void checkAddress() {
		if (StringUtil.validBicoinAddress(etAddress.getText().toString())) {
			ibtnDelete.setVisibility(View.VISIBLE);
			ibtnScan.setVisibility(View.GONE);
			etAddress.setEnabled(false);
			ViewParent parent = getParent();
			if (parent != null && parent instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) parent;
				View lastChild = group.getChildAt(group.getChildCount() - 1);
				if (lastChild == this) {
					group.addView(new AddAddressListItem(activity, listener),
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
				}
			}
			if (listener != null) {
				listener.onAddressChanged();
			}
		} else {
			ibtnDelete.setVisibility(View.GONE);
			ibtnScan.setVisibility(View.VISIBLE);
			etAddress.setEnabled(true);
		}
	}

	private int getPositionInParent() {
		ViewParent parent = getParent();
		if (parent != null && parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			int childCount = group.getChildCount();
			for (int i = 0; i < childCount; i++) {
				if (group.getChildAt(i) == this) {
					return i;
				}
			}
		}
		return -1;
	}

	private OnClickListener deleteClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ViewParent parent = getParent();
			if (parent != null && parent instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) parent;
				group.removeView(AddAddressListItem.this);
				if (listener != null) {
					listener.onAddressChanged();
				}
			}
		}
	};

}
