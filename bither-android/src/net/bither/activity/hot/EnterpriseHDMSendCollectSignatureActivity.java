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

package net.bither.activity.hot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.EnterpriseHDMTxSignaturePool;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.LogUtil;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/6/13.
 */
public class EnterpriseHDMSendCollectSignatureActivity extends SwipeRightFragmentActivity {
    private static final int SignCode = 1029;

    private static final String ChangeAddressTag = "ChangeAddress";
    private static final String IndexTag = "Index";

    private static EnterpriseHDMTxSignaturePool pubPool;

    private EnterpriseHDMTxSignaturePool pool;

    private TextView tvSignatureNeeded;
    private LinearLayout llSignatures;

    private String changeAddress;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pool = pubPool;
        pubPool = null;
        if (pool == null) {
            finish();
            return;
        }
        changeAddress = getIntent().getStringExtra(ChangeAddressTag);
        index = getIntent().getIntExtra(IndexTag, 0);
        setContentView(R.layout.activity_enterprise_hdm_send_collect_signature);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        findViewById(R.id.btn_add).setOnClickListener(addClick);
        tvSignatureNeeded = (TextView) findViewById(R.id.tv_signature_needed);
        llSignatures = (LinearLayout) findViewById(R.id.ll_signatures);
        tvSignatureNeeded.setText(String.format(getString(R.string
                .enterprise_hdm_keychain_payment_proposal_signature_needed), pool.threshold(),
                pool.pubCount()));
        llSignatures.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0;
             i < pool.threshold();
             i++) {
            inflater.inflate(R.layout.list_item_enterprise_hdm_collector, llSignatures, true);
        }
    }

    private View.OnClickListener addClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(EnterpriseHDMSendCollectSignatureActivity.this,
                    UnsignedTxQrCodeActivity.class);
            String content = QRCodeTxTransport.getPresignTxString(pool.tx(), changeAddress,
                    getString(R.string.address_cannot_be_parsed), index, QRCodeTxTransport
                            .TxTransportType.ColdHDM);
            intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, content);
            if (!Utils.isEmpty(changeAddress)) {
                intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_HAS_CHANGE_ADDRESS_STRING, true);
            }
            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string
                    .unsigned_transaction_qr_code_title));
            startActivityForResult(intent, SignCode);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SignCode) {
            if (resultCode == RESULT_OK) {
                final String qr = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (Utils.isEmpty(qr)) {
                    LogUtil.w("EnterpriseHDM", "EnterpriseHDMSendCollectSignature qr empty");
                    return;
                }
                addSignature(qr);
                showSignatures();
                if (pool.satisfied()) {
                    sign();
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addSignature(String qr) {
        boolean success;
        try {
            String[] stringArray = QRCodeUtil.splitString(qr);
            ArrayList<byte[]> sigs = new ArrayList<byte[]>();
            for (String str : stringArray) {
                if (!Utils.isEmpty(str)) {
                    sigs.add(Utils.hexStringToByteArray(str));
                }
            }
            success = pool.addSignature(sigs);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        if (!success) {
            DropdownMessage.showDropdownMessage(this, R.string
                    .enterprise_hdm_keychain_payment_proposal_sign_failed);
        }
    }

    private void showSignatures() {
        for (int i = 0;
             i < pool.threshold();
             i++) {
            View v = llSignatures.getChildAt(i);
            v.setBackgroundColor(i < pool.signatureCount() ? getResources().getColor(R.color
                    .blue_dark) : getResources().getColor(R.color.transparent));
        }
    }

    private void sign() {
        final DialogProgress dp = new DialogProgress(this, R.string.please_wait);
        dp.show();
        new Thread() {
            @Override
            public void run() {
                Tx t = null;
                try {
                    t = pool.sign();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final Tx tx = t;
                if (tx != null) {
                    try {
                        PeerManager.instance().publishTransaction(tx);
                    } catch (PeerManager.PublishUnsignedTxException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                DropdownMessage.showDropdownMessage
                                        (EnterpriseHDMSendCollectSignatureActivity
                                        .this, R.string.send_failed);
                            }
                        });
                        return;
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dp.dismiss();
                        if (tx != null) {
                            findViewById(R.id.btn_add).setEnabled(false);
                            DropdownMessage.showDropdownMessage
                                    (EnterpriseHDMSendCollectSignatureActivity.this, R.string
                                            .enterprise_hdm_keychain_payment_proposal_sign_success, new Runnable() {
                                @Override
                                public void run() {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        } else {
                            pool.clearSignatures();
                            DropdownMessage.showDropdownMessage
                                    (EnterpriseHDMSendCollectSignatureActivity.this, R.string
                                            .enterprise_hdm_keychain_payment_proposal_sign_failed);
                            showSignatures();
                        }
                    }
                });
            }
        }.start();
    }

    public static Intent start(Context context, EnterpriseHDMTxSignaturePool pool, String
            changeAddress, int index) {
        Intent intent = new Intent(context, EnterpriseHDMSendCollectSignatureActivity.class);
        if (!Utils.isEmpty(changeAddress)) {
            intent.putExtra(ChangeAddressTag, changeAddress);
        }
        intent.putExtra(IndexTag, index);
        pubPool = pool;
        return intent;
    }
}
