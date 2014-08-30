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
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Tx;
import net.bither.util.GenericUtils;

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

    public DialogSendConfirm(Context context, Tx tx, SendConfirmListener listener) {
        super(context);
        this.tx = tx;
        this.listener = listener;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_send_confirm);
        TextView tvAddress = (TextView) findViewById(R.id.tv_address);
        TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
        TextView tvFee = (TextView) findViewById(R.id.tv_fee);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);
        Button btnOk = (Button) findViewById(R.id.btn_ok);
        TextView tvLowPriorityWarn = (TextView) findViewById(R.id.tv_low_priority_warn);
        tvAddress.setText(tx.getFirstOutAddress());
        tvBtc.setText(GenericUtils.formatValueWithBold(tx.amountSentToAddress(tx.getFirstOutAddress())));
        tvFee.setText(GenericUtils.formatValueWithBold(tx.getFee()));
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
