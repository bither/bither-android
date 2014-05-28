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
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.model.BitherAddress;
import net.bither.util.ConfidenceUtil;
import net.bither.util.DateTimeUtil;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;

import java.math.BigInteger;
import java.util.Date;

/**
 * Created by songchenwen on 14-5-28.
 */
public class TransactionListItem extends FrameLayout implements
        MarketTickerChangedObserver {

    private Activity activity;
    private TransactionImmutureConfidenceIconView vConfidenceIcon;
    private ImageButton ibtnAddressFull;
    private TextView tvTransactionAddress;
    private BtcToMoneyButton btnBtc;
    private TextView tvTime;

    private Transaction transaction;
    private OnClickListener confidenceClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (transaction != null) {
                DialogTransactionConfidence dialog = new DialogTransactionConfidence(
                        getContext(),
                        ConfidenceUtil.getDepthInChain(transaction
                                .getConfidence())
                );
                dialog.show(v);
            }
        }
    };
    private BitherAddress address;
    private OnClickListener addressFullClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            ArrayMap<String, BigInteger> addresses = new ArrayMap<String, BigInteger>();
            boolean isIncoming = true;
            try {
                isIncoming = transaction.getValue(address).compareTo(
                        BigInteger.ZERO) >= 0;
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            int subCount = isIncoming ? transaction.getInputs().size()
                    : transaction.getOutputs().size();
            String subAddress = null;
            BigInteger value;
            for (int i = 0;
                 i < subCount;
                 i++) {
                subAddress = null;
                if (isIncoming) {
                    if (transaction.getInput(i).isCoinBase()) {
                        subAddress = getContext().getResources().getString(
                                R.string.input_coinbase);
                    } else {
                        try {
                            subAddress = transaction.getInput(i)
                                    .getFromAddress().toString();
                        } catch (ScriptException e) {
                            e.printStackTrace();
                        }
                    }
                    TransactionOutput outConnected = transaction.getInput(i)
                            .getConnectedOutput();
                    if (outConnected != null) {
                        value = outConnected.getValue();
                    } else {
                        value = null;
                    }
                } else {
                    TransactionOutput out = transaction.getOutput(i);
                    value = out.getValue();
                    try {
                        subAddress = GenericUtils.addressFromScriptPubKey(
                                out.getScriptPubKey(),
                                address.getNetworkParameters());
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }
                }
                if (subAddress == null
                        || StringUtil.checkAddressIsNull(subAddress)) {
                    subAddress = getContext().getResources().getString(
                            R.string.address_cannot_be_parsed);
                }
                if (StringUtil.compareString(subAddress, address.getAddress())) {
                    subAddress = getContext().getString(R.string.address_mine);
                }
                addresses.put(subAddress, value);
            }
            DialogAddressFull dialog = new DialogAddressFull(activity,
                    addresses);
            dialog.show(v);
        }
    };

    public TransactionListItem(Activity activity) {
        super(activity);
        this.activity = activity;
        initView();
    }

    private void initView() {
        removeAllViews();
        addView(LayoutInflater.from(activity).inflate(
                        R.layout.list_item_address_detail_transaction, null),
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );
        vConfidenceIcon = (TransactionImmutureConfidenceIconView) findViewById(R.id
                .fl_confidence_icon);
        tvTransactionAddress = (TextView) findViewById(R.id.tv_transaction_address);
        btnBtc = (BtcToMoneyButton) findViewById(R.id.btn_btc);
        tvTime = (TextView) findViewById(R.id.tv_time);
        ibtnAddressFull = (ImageButton) findViewById(R.id.ibtn_address_full);
        ibtnAddressFull.setOnClickListener(addressFullClick);
        vConfidenceIcon.setOnClickListener(confidenceClick);
    }

    private TransactionListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private TransactionListItem(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTransaction(Transaction transaction, BitherAddress address) {
        this.transaction = transaction;
        this.address = address;
        showTransaction();
    }

    private void showTransaction() {
        if (this.transaction == null || address == null) {
            return;
        }
        BigInteger value;
        try {
            value = transaction.getValue(address);
        } catch (Exception e) {
            return;
        }
        vConfidenceIcon.setConfidence(transaction.getConfidence());
        boolean isReceived = value.compareTo(BigInteger.ZERO) >= 0;
        btnBtc.setBigInteger(value);
        Date time = transaction.getUpdateTime();
        if (time.equals(new Date(0))) {
            tvTime.setText("");
        } else {
            tvTime.setText(DateTimeUtil.getDateTimeString(transaction
                    .getUpdateTime()));
        }
        if (isReceived) {
            if (transaction.getInput(0).isCoinBase()) {
                tvTransactionAddress.setText(R.string.input_coinbase);
            } else {
                try {
                    String subAddress = transaction.getInput(0)
                            .getFromAddress().toString();
                    if (StringUtil.checkAddressIsNull(subAddress)) {
                        tvTransactionAddress
                                .setText(BitherSetting.UNKONW_ADDRESS_STRING);
                    } else {
                        tvTransactionAddress.setText(StringUtil
                                .shortenAddress(subAddress));
                    }
                } catch (ScriptException e) {
                    e.printStackTrace();
                    tvTransactionAddress
                            .setText(BitherSetting.UNKONW_ADDRESS_STRING);
                }
            }
        } else {
            TransactionOutput out = transaction.getOutput(0);
            value = out.getValue();
            String subAddress = null;
            try {
                subAddress = GenericUtils.addressFromScriptPubKey(
                        out.getScriptPubKey(), address.getNetworkParameters());
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            if (subAddress != null) {
                tvTransactionAddress.setText(StringUtil
                        .shortenAddress(subAddress));
            } else {
                tvTransactionAddress
                        .setText(BitherSetting.UNKONW_ADDRESS_STRING);
            }
        }

    }

    public void onPause() {
        vConfidenceIcon.onPause();
        btnBtc.onPause();
    }

    public void onResume() {
        vConfidenceIcon.onResume();
        btnBtc.onResume();
    }

    @Override
    public void onMarketTickerChanged() {
        btnBtc.onMarketTickerChanged();

    }
}
