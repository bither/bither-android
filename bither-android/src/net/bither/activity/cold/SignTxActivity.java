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

package net.bither.activity.cold;

import java.util.ArrayList;
import java.util.List;

import net.bither.BitherSetting;
import net.bither.QrCodeActivity;
import net.bither.R;
import net.bither.ScanActivity;
import net.bither.ScanQRCodeTransportActivity;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.QRCodeTxTransport;
import net.bither.ui.base.DialogPassword;
import net.bither.ui.base.DialogPassword.DialogPasswordListener;
import net.bither.ui.base.DialogProgress;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.BackClickListener;
import net.bither.util.GenericUtils;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ECKey.ECDSASignature;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;

public class SignTxActivity extends SwipeRightActivity implements
		DialogPasswordListener {

	private TextView tvFrom;
	private TextView tvTo;
	private TextView tvAmount;
	private TextView tvFee;
	private Button btnSign;
	private TextView tvCannotFindPrivateKey;

	private QRCodeTxTransport qrCodeTransport;

	private DialogProgress dp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_tx);
		toScanActivity();
		initView();
	}

	private void initView() {
		findViewById(R.id.ibtn_cancel).setOnClickListener(
				new BackClickListener(0, R.anim.slide_out_right));
		tvFrom = (TextView) findViewById(R.id.tv_address_from);
		tvTo = (TextView) findViewById(R.id.tv_address_to);
		tvAmount = (TextView) findViewById(R.id.tv_amount);
		tvFee = (TextView) findViewById(R.id.tv_fee);
		btnSign = (Button) findViewById(R.id.btn_sign);
		tvCannotFindPrivateKey = (TextView) findViewById(R.id.tv_can_not_find_private_key);
		btnSign.setEnabled(false);
		btnSign.setOnClickListener(signClick);
		dp = new DialogProgress(this, R.string.signing_transaction);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE
				&& resultCode == Activity.RESULT_OK) {
			String str = data.getExtras().getString(
					ScanActivity.INTENT_EXTRA_RESULT);
			qrCodeTransport = QRCodeTxTransport.formatQRCodeTransport(str);
			if (qrCodeTransport != null) {
				showTransaction();
			} else {
				super.finish();
			}
		} else {
			super.finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void showTransaction() {
		tvFrom.setText(qrCodeTransport.getMyAddress());
		tvTo.setText(qrCodeTransport.getToAddress());
		tvAmount.setText(GenericUtils.formatValueWithBold(qrCodeTransport
				.getTo()));
		tvFee.setText(GenericUtils.formatValueWithBold(qrCodeTransport.getFee()));
		BitherAddressWithPrivateKey address = WalletUtils
				.findPrivateKey(qrCodeTransport.getMyAddress());
		if (address == null) {
			btnSign.setEnabled(false);
			tvCannotFindPrivateKey.setVisibility(View.VISIBLE);
		} else {
			btnSign.setEnabled(true);
			tvCannotFindPrivateKey.setVisibility(View.GONE);
		}
	}

	private OnClickListener signClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			DialogPassword dialogPassword = new DialogPassword(
					SignTxActivity.this, SignTxActivity.this);
			dialogPassword.show();
		}
	};

	@Override
	public void onPasswordEntered(final String password) {
		Thread thread = new Thread() {
			public void run() {
				BitherAddressWithPrivateKey address = WalletUtils
						.findPrivateKey(qrCodeTransport.getMyAddress());
				List<String> strings = signHash(address, password);
				String result = "";
				for (int i = 0; i < strings.size(); i++) {
					if (i < strings.size() - 1) {
						result = result + strings.get(i)
								+ StringUtil.QR_CODE_SPLIT;
					} else {
						result = result + strings.get(i);
					}
				}
				final String r = result;
				dp.setThread(null);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dp.dismiss();
						Intent intent = new Intent(SignTxActivity.this,
								QrCodeActivity.class);
						intent.putExtra(
								BitherSetting.INTENT_REF.QR_CODE_STRING, r);
						intent.putExtra(
								BitherSetting.INTENT_REF.TITLE_STRING,
								getString(R.string.signed_transaction_qr_code_title));
						startActivity(intent);
						finish();
					}
				});
			};
		};
		dp.setThread(thread);
		thread.start();
		dp.show();
	}

	private List<String> signHash(
			BitherAddressWithPrivateKey bitherAddressWithPrivateKey,
			String password) {
		ECKey ecKey = bitherAddressWithPrivateKey.getKeys().get(0);
		List<String> strings = new ArrayList<String>();
		for (String hashStr : qrCodeTransport.getHashList()) {
			Sha256Hash hash = new Sha256Hash(hashStr);
			ECDSASignature eSignature = ecKey.sign(
					hash,
					bitherAddressWithPrivateKey.getKeyCrypter().deriveKey(
							password));
			TransactionSignature transactionSignature = new TransactionSignature(
					eSignature, Transaction.SigHash.ALL, false);
			Script script = ScriptBuilder.createInputScript(
					transactionSignature, ecKey);
			strings.add(Utils.bytesToHexString(script.getProgram()));
		}
		return strings;
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.slide_out_right);
	}

	private void toScanActivity() {
		Intent intent = new Intent(SignTxActivity.this,
				ScanQRCodeTransportActivity.class);
		intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
				getString(R.string.scan_unsigned_transaction_title));
		startActivityForResult(intent,
				BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
	}

}
