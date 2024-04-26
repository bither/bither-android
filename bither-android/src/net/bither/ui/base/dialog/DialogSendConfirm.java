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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Coin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

public class DialogSendConfirm extends CenterDialog implements OnDismissListener {
    public static interface SendConfirmListener {
        public void onConfirm(Tx request);

        public void onCancel();
    }

    private static final BigInteger MinHighPriorityOutput = BigInteger.valueOf(1000000);
    private boolean confirmed = false;
    private Tx tx;
    private SendConfirmListener listener;

    public DialogSendConfirm(Context context, Tx tx, String changeAddress,
                             SendConfirmListener listener) {
        super(context);
        this.tx = tx;
        this.listener = listener;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_send_confirm);
        TextView tvAddress = (TextView) findViewById(R.id.tv_address);
        TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
        TextView tvFee = (TextView) findViewById(R.id.tv_fee);
        TextView tvSymbol = (TextView) findViewById(R.id.tv_symbol);
        TextView tvFeeSymbol = (TextView) findViewById(R.id.tv_fee_symbol);
        View llChange = findViewById(R.id.ll_change);
        TextView tvAddressChange = (TextView) findViewById(R.id.tv_address_change);
        TextView tvBtcChange = (TextView) findViewById(R.id.tv_btc_change);
        TextView tvSymbolChange = (TextView) findViewById(R.id.tv_symbol_change);
        String symbol = AppSharedPreference.getInstance().getBitcoinUnit().name();
        tvSymbol.setText(symbol);
        tvFeeSymbol.setText(symbol);
        tvSymbolChange.setText(symbol);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);
        Button btnOk = (Button) findViewById(R.id.btn_ok);
        String outAddress = tx.getFirstOutAddressOtherThanChange(changeAddress);
        if (Utils.isEmpty(changeAddress) || tx.amountSentToAddress(changeAddress) <= 0) {
            llChange.setVisibility(View.GONE);
        } else {
            tvAddressChange.setText(WalletUtils.formatHash(changeAddress, 4, 24));
            tvBtcChange.setText(UnitUtilWrapper.formatValueWithBold(tx.amountSentToAddress(changeAddress)));
        }
        if (tx.getFirstOutAddress() != null) {
            tvAddress.setText(WalletUtils.formatHash(outAddress, 4, 24));
        }
        tvBtc.setText(UnitUtilWrapper.formatValueWithBold(tx.amountSentToAddress(outAddress)));
        tvFee.setText(UnitUtilWrapper.formatValueWithBold(tx.getFee()));
        if (tx.getCoin() == Coin.BTC && tx.getEstimationTxSize() > 0) {
            LinearLayout llFeeRate = findViewById(R.id.ll_fee_rate);
            TextView tvFeeRate = findViewById(R.id.tv_fee_rate);
            tvFeeRate.setText(String.format("%.2f", tx.getFee() / (float) (tx.getEstimationTxSize())).replaceAll("\\.0*$", ""));
            llFeeRate.setVisibility(View.VISIBLE);
        }
        // This warning is no longer needed. As more and more mining pool upgrade their
        // bitcoin client to 0.9.+, low fee transactions get confirmed soon enough.
//		if (isLowPriority(tx)) {
//			tvLowPriorityWarn.setVisibility(View.VISIBLE);
//		} else {
//			tvLowPriorityWarn.setVisibility(View.GONE);
//		}
        btnCancel.setOnClickListener(cancelClick);
        btnOk.setOnClickListener(okClick);
    }

    private View.OnClickListener cancelClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            confirmed = false;
            dismiss();
        }
    };
    private View.OnClickListener okClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            confirmed = true;
            dismiss();
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (listener != null) {
            if (confirmed) {
                listener.onConfirm(tx);
            } else {
                listener.onCancel();
            }
        }
    }
}
