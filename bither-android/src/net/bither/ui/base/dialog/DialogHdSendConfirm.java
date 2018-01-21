/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Coin;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.preference.AppSharedPreference;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.util.List;

/**
 * Created by songchenwen on 15/4/17.
 */
public class DialogHdSendConfirm extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener {
    public interface SendConfirmListener {
        void onConfirm();

        void onCancel();
    }

    private boolean confirmed = false;
    private SendConfirmListener listener;

    public DialogHdSendConfirm(Context context, String toAddress, Tx tx,
                               SendConfirmListener listener) {
        this(context, toAddress, tx, Coin.BTC, listener);
    }

    public DialogHdSendConfirm(Context context, String toAddress, Tx tx, Coin coin,
                               SendConfirmListener listener) {
        super(context);
        this.listener = listener;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_send_confirm);
        TextView tvAddress = (TextView) findViewById(R.id.tv_address);
        TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
        TextView tvFee = (TextView) findViewById(R.id.tv_fee);
        TextView tvSymbol = (TextView) findViewById(R.id.tv_symbol);
        TextView tvFeeSymbol = (TextView) findViewById(R.id.tv_fee_symbol);
        View llChange = findViewById(R.id.ll_change);
        TextView tvSymbolChange = (TextView) findViewById(R.id.tv_symbol_change);
        if (coin == Coin.BTC) {
            String symbol = AppSharedPreference.getInstance().getBitcoinUnit().name();
            tvSymbol.setText(symbol);
            tvFeeSymbol.setText(symbol);
            tvSymbolChange.setText(symbol);
        } else {
            String text = coin.getSplitCoin().getName();
            tvSymbol.setText(text);
            tvFeeSymbol.setText(text);
            tvSymbolChange.setText(text);
        }
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        llChange.setVisibility(View.GONE);
        tvAddress.setText(WalletUtils.formatHash(toAddress, 4, 24));
        tvBtc.setText(UnitUtilWrapper.formatValueWithBold(tx.amountSentToAddress(toAddress)));
        tvFee.setText(UnitUtilWrapper.formatValueWithBold(tx.getFee()));
    }

    public DialogHdSendConfirm(Context context, String toAddress, List<Tx> txs, SendConfirmListener listener, SplitCoin splitCoin) {
        super(context);
        this.listener = listener;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_send_confirm);
        TextView tvAddress = (TextView) findViewById(R.id.tv_address);
        TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
        TextView tvFee = (TextView) findViewById(R.id.tv_fee);
        TextView tvSymbol = (TextView) findViewById(R.id.tv_symbol);
        TextView tvFeeSymbol = (TextView) findViewById(R.id.tv_fee_symbol);
        View llChange = findViewById(R.id.ll_change);
        TextView tvSymbolChange = (TextView) findViewById(R.id.tv_symbol_change);
        tvSymbol.setText(splitCoin.getName());
        tvFeeSymbol.setText(splitCoin.getName());
        tvSymbolChange.setText(splitCoin.getName());
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        llChange.setVisibility(View.GONE);
        tvAddress.setText(WalletUtils.formatHash(toAddress, 4, 24));
        long amount = 0;
        long fee = 0;
        for (Tx tx: txs) {
            amount += tx.amountSentToAddress(toAddress);
            fee += tx.getFee();
        }
        tvBtc.setText(UnitUtilWrapper.formatValueWithBold(amount,splitCoin.getCoin()));
        tvFee.setText(UnitUtilWrapper.formatValueWithBold(fee,splitCoin.getCoin()));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (listener != null) {
            if (confirmed) {
                listener.onConfirm();
            } else {
                listener.onCancel();
            }
        }
    }

    @Override
    public void onClick(View v) {
        confirmed = v.getId() == R.id.btn_ok;
        dismiss();
    }
}
