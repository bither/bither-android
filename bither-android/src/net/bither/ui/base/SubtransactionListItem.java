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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

public class SubtransactionListItem extends FrameLayout implements View.OnClickListener {
    public static final int Height = UIUtil.dip2pix(70);
    public static final int MessageHeight = UIUtil.dip2pix(40);

    private View parent;
    private TextView tvAddress;
    private TextView tvBtc;
    private TextView tvMessage;
    private FrameLayout flAddress;
    private String address;
    private Activity activity;

    public SubtransactionListItem(Activity context) {
        super(context);
        activity = context;
        removeAllViews();
        parent = LayoutInflater.from(context).inflate(R.layout.list_item_transaction_address, null);
        addView(parent, LayoutParams.MATCH_PARENT, Height);
        tvAddress = (TextView) findViewById(R.id.tv_subtransaction_address);
        tvBtc = (TextView) findViewById(R.id.tv_subtransaction_btc);
        tvMessage = (TextView) findViewById(R.id.tv_message);
        flAddress = (FrameLayout) findViewById(R.id.fl_address);
        flAddress.setOnClickListener(this);
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
        tvMessage.setTextColor(color);
        tvBtc.setTextColor(color);
    }

    public void setContent(String address, long value) {
        if (!isMessage(address)) {
            this.address = address;
            parent.getLayoutParams().height = Height;
            tvAddress.setText(WalletUtils.formatHash(address, 4, 12));
            tvMessage.setVisibility(View.GONE);
            flAddress.setVisibility(View.VISIBLE);
        } else {
            parent.getLayoutParams().height = MessageHeight;
            tvMessage.setText(address);
            flAddress.setVisibility(View.GONE);
            tvMessage.setVisibility(View.VISIBLE);
        }
        if (value != 0) {
            tvBtc.setText(GenericUtils.formatValue(value));
        } else {
            tvBtc.setText("");
        }
    }

    public static boolean isMessage(String address) {
        return StringUtil.compareString(address, BitherApplication.mContext.getString(R.string
                .address_cannot_be_parsed)) || StringUtil.compareString(address,
                BitherApplication.mContext.getString(R.string.input_coinbase)) || StringUtil
                .compareString(address, BitherApplication.mContext.getString(R.string
                        .address_mine));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fl_address) {
            if (address != null && !isMessage(address)) {
                StringUtil.copyString(address);
                DropdownMessage.showDropdownMessage(activity,
                        R.string.copy_address_success);
            }
        }
    }
}
