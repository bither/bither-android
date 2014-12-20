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

package net.bither;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by songchenwen on 14/12/19.
 */
public class VerifyMessageSignatureActivity extends SwipeRightFragmentActivity {
    private static final int AddressRequestCode = 1059, MessageRequestCode = 1100, SignatureRequestCode = 1101;

    private InputMethodManager imm;
    private EditText etAddress, etMessage, etSignature;
    private Button btnQrAddrss, btnQrMessage, btnQrSignature, btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_verify_message_signature);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        etAddress = (EditText) findViewById(R.id.et_address);
        etMessage = (EditText) findViewById(R.id.et_message);
        etSignature = (EditText) findViewById(R.id.et_signature);
        btnQrAddrss = (Button) findViewById(R.id.btn_qr_address);
        btnQrMessage = (Button) findViewById(R.id.btn_qr_message);
        btnQrSignature = (Button) findViewById(R.id.btn_qr_signature);
        btnVerify = (Button) findViewById(R.id.btn_verify);
        btnQrAddrss.setOnClickListener(qrClick);
        btnQrMessage.setOnClickListener(qrClick);
        btnQrSignature.setOnClickListener(qrClick);
        btnVerify.setOnClickListener(verifyClick);
    }

    private View.OnClickListener verifyClick = new View.OnClickListener() {
        private boolean signed;

        @Override
        public void onClick(View v) {
            final String address = etAddress.getText().toString().trim();
            final String message = etMessage.getText().toString().trim();
            final String signature = etSignature.getText().toString().trim();
            if (Utils.isEmpty(address) || Utils.isEmpty(message) || Utils.isEmpty(signature)) {
                return;
            }
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            final DialogProgress dp = new DialogProgress(VerifyMessageSignatureActivity.this,
                    R.string.please_wait);
            dp.setCanceledOnTouchOutside(false);
            dp.show();
            new Thread() {
                @Override
                public void run() {
                    signed = PrivateKeyUtil.verifyMessage(address, message, signature);
                    btnVerify.post(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(VerifyMessageSignatureActivity
                                            .this,
                                    signed ? R.string.verify_message_signature_verify_success : R
                                            .string.verify_message_signature_verify_failed);
                        }
                    });
                }
            }.start();
        }
    };

    private View.OnClickListener qrClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int requestCode;
            switch (v.getId()) {
                case R.id.btn_qr_address:
                    requestCode = AddressRequestCode;
                    break;
                case R.id.btn_qr_message:
                    requestCode = MessageRequestCode;
                    break;
                case R.id.btn_qr_signature:
                default:
                    requestCode = SignatureRequestCode;
                    break;
            }
            Intent intent = new Intent(VerifyMessageSignatureActivity.this, ScanActivity.class);
            startActivityForResult(intent, requestCode);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (Utils.isEmpty(input)) {
                return;
            }
            input = input.trim();
            switch (requestCode) {
                case AddressRequestCode:
                    etAddress.setText(input);
                    break;
                case MessageRequestCode:
                    etMessage.setText(input);
                    break;
                case SignatureRequestCode:
                    etSignature.setText(input);
                    break;
                default:
                    break;
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
