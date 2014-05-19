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

import java.math.BigInteger;
import java.util.List;

import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.util.GenericUtils;
import net.bither.util.TransactionsUtil.TransactionFeeMode;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.params.MainNetParams;

public class DialogSendConfirm extends CenterDialog implements
		OnDismissListener {
	public static interface SendConfirmListener {
		public void onConfirm(SendRequest request);

		public void onCancel();
	}

	private static final BigInteger MinHighPriorityOutput = BigInteger
			.valueOf(1000000);
	private boolean confirmed = false;
	private SendRequest request;
	private SendConfirmListener listener;

	public DialogSendConfirm(Context context, SendRequest request,
			SendConfirmListener listener) {
		super(context);
		this.request = request;
		this.listener = listener;
		setOnDismissListener(this);
		setContentView(R.layout.dialog_send_confirm);
		TextView tvAddress = (TextView) findViewById(R.id.tv_address);
		TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
		TextView tvFee = (TextView) findViewById(R.id.tv_fee);
		Button btnCancel = (Button) findViewById(R.id.btn_cancel);
		Button btnOk = (Button) findViewById(R.id.btn_ok);
		TextView tvLowPriorityWarn = (TextView) findViewById(R.id.tv_low_priority_warn);
		TransactionOutput out = request.tx.getOutput(0);
		try {
			tvAddress.setText(GenericUtils.addressFromScriptPubKey(
					out.getScriptPubKey(), MainNetParams.get()));
		} catch (ScriptException e) {
			tvAddress.setText(R.string.address_cannot_be_parsed);
			e.printStackTrace();
		}
		tvBtc.setText(GenericUtils.formatValueWithBold(out.getValue()));
		tvFee.setText(GenericUtils.formatValueWithBold(request.fee));
		if (isLowPriority(request.tx)) {
			tvLowPriorityWarn.setVisibility(View.VISIBLE);
		} else {
			tvLowPriorityWarn.setVisibility(View.GONE);
		}
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
				listener.onConfirm(request);
			} else {
				listener.onCancel();
			}
		}
	};

	private boolean isLowPriority(Transaction tx) {
		if (AppSharedPreference.getInstance().getTransactionFeeMode().ordinal() == TransactionFeeMode.Low
				.ordinal()) {
			List<TransactionOutput> outs = tx.getOutputs();
			for (TransactionOutput out : outs) {
				if (out.getValue().subtract(MinHighPriorityOutput).signum() < 0) {
					return true;
				}
			}
		}
		return false;
	}
}
