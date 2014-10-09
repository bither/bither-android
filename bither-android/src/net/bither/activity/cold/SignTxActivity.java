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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.QRCodeUtil;
import net.bither.qrcode.QRCodeActivity;
import net.bither.qrcode.QRCodeEnodeUtil;
import net.bither.qrcode.QRCodeTxTransport;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.GenericUtils;
import net.bither.util.SecureCharSequence;
import net.bither.util.WalletUtils;

import java.util.List;

public class SignTxActivity extends SwipeRightActivity implements
        IDialogPasswordListener {

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
                new IBackClickListener(0, R.anim.slide_out_right));
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
            qrCodeTransport = QRCodeEnodeUtil.formatQRCodeTransport(str);
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
        tvFrom.setText(WalletUtils.formatHash(qrCodeTransport.getMyAddress(), 4, qrCodeTransport.getMyAddress().length()));
        tvTo.setText(WalletUtils.formatHash(qrCodeTransport.getToAddress(), 4, qrCodeTransport.getToAddress().length()));
        tvAmount.setText(GenericUtils.formatValueWithBold(qrCodeTransport
                .getTo()));
        tvFee.setText(GenericUtils.formatValueWithBold(qrCodeTransport.getFee()));
        Address address = WalletUtils
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
    public void onPasswordEntered(final SecureCharSequence password) {
        Thread thread = new Thread() {
            public void run() {
                Address address = WalletUtils.findPrivateKey(qrCodeTransport.getMyAddress());
                List<String> strings = address.signStrHashes(qrCodeTransport.getHashList(), password);
                password.wipe();
                String result = "";
                for (int i = 0;
                     i < strings.size();
                     i++) {
                    if (i < strings.size() - 1) {
                        result = result + strings.get(i) + QRCodeUtil.QR_CODE_SPLIT;
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
                        Intent intent = new Intent(SignTxActivity.this, QRCodeActivity.class);
                        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, r);
                        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string.signed_transaction_qr_code_title));
                        startActivity(intent);
                        finish();
                    }
                });
            }

            ;
        };
        dp.setThread(thread);
        thread.start();
        dp.show();
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
