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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SignTxActivity extends SwipeRightActivity implements
        IDialogPasswordListener {

    private TextView tvFrom;
    private TextView tvTo;
    private TextView tvAmount;
    private TextView tvFee;
    private TextView tvSymbol;
    private TextView tvFeeSymbol;
    private View llChange;
    private TextView tvAddressChange;
    private TextView tvAmountChange;
    private TextView tvSymbolChange;

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
        llChange = findViewById(R.id.ll_change);
        tvAddressChange = (TextView) findViewById(R.id.tv_address_change);
        tvAmountChange = (TextView) findViewById(R.id.tv_amount_change);
        tvSymbolChange = (TextView) findViewById(R.id.tv_symbol_change);
        btnSign = (Button) findViewById(R.id.btn_sign);
        tvCannotFindPrivateKey = (TextView) findViewById(R.id.tv_can_not_find_private_key);
        tvSymbol = (TextView) findViewById(R.id.tv_symbol);
        tvFeeSymbol = (TextView) findViewById(R.id.tv_fee_symbol);
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
        String symbol = AppSharedPreference.getInstance().getBitcoinUnit().name();
        tvSymbol.setText(symbol);
        tvFeeSymbol.setText(symbol);
        tvSymbolChange.setText(symbol);
        if (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType.ColdHD) {
            tvFrom.setText(R.string.address_group_hd_monitored);
        } else {
            tvFrom.setText(WalletUtils.formatHash(qrCodeTransport.getMyAddress(), 4,
                    qrCodeTransport.getMyAddress().length()));
        }
        tvTo.setText(WalletUtils.formatHash(qrCodeTransport.getToAddress(), 4, qrCodeTransport.getToAddress().length()));
        tvAmount.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getTo()));
        tvFee.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getFee()));
        llChange.setVisibility(View.GONE);
        if(!Utils.isEmpty(qrCodeTransport.getChangeAddress())){
            llChange.setVisibility(View.VISIBLE);
            tvAddressChange.setText(WalletUtils.formatHash(qrCodeTransport.getChangeAddress(), 4, qrCodeTransport.getChangeAddress().length()));
            tvAmountChange.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getChangeAmt()));
        }
        Address address = WalletUtils
                .findPrivateKey(qrCodeTransport.getMyAddress());
        if ((qrCodeTransport.getHdmIndex() < 0 && address == null) || (qrCodeTransport
                .getHdmIndex() >= 0 && qrCodeTransport.getTxTransportType() != QRCodeTxTransport
                .TxTransportType.ColdHDM && !AddressManager.getInstance().hasHDMKeychain()) ||
                (qrCodeTransport.getHdmIndex() >= 0 && qrCodeTransport.getTxTransportType() ==
                        QRCodeTxTransport.TxTransportType.ColdHDM && !EnterpriseHDMSeed.hasSeed()
                ) || (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType
                .ColdHD && !HDAccountCold.hasHDAccountCold())) {
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
                List<String> strings = null;
                if (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType
                        .ColdHD) {
                    HDAccountCold account = HDAccountCold.hdAccountCold();
                    try {
                        List<byte[]> bytes = account.signHashHexes(qrCodeTransport.getHashList(),
                                qrCodeTransport.getPathTypeIndexes(), password);
                        strings = new ArrayList<String>(Collections2.transform(bytes, new
                                Function<byte[], String>() {
                            @Nullable
                            @Override
                            public String apply(byte[] input) {
                                return Utils.bytesToHexString(input);
                            }
                        }));
                    } catch (Exception e) {
                        e.printStackTrace();
                        dp.setThread(null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                DropdownMessage.showDropdownMessage(SignTxActivity.this, R.string
                                        .unsigned_transaction_sign_failed);
                            }
                        });
                        password.wipe();
                        return;
                    }


                } else if (qrCodeTransport.getHdmIndex() >= 0) {
                    if (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType
                            .ColdHDM) {
                        if (!EnterpriseHDMSeed.hasSeed()) {
                            dp.setThread(null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dp.dismiss();
                                    DropdownMessage.showDropdownMessage(SignTxActivity.this, R
                                            .string.hdm_send_with_cold_no_requested_seed);
                                }
                            });
                            password.wipe();
                            return;
                        }
                        ArrayList<byte[]> unsigns = new ArrayList<byte[]>();
                        for (String hash : qrCodeTransport.getHashList()) {
                            unsigns.add(Utils.hexStringToByteArray(hash));
                        }
                        try {
                            List<byte[]> sigs = EnterpriseHDMSeed.seed().signHashes
                                    (qrCodeTransport.getHdmIndex(), unsigns, password);
                            strings = new ArrayList<String>();
                            for (byte[] sig : sigs) {
                                strings.add(Utils.bytesToHexString(sig));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            dp.setThread(null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dp.dismiss();
                                    DropdownMessage.showDropdownMessage(SignTxActivity.this, R
                                            .string.hdm_send_with_cold_no_requested_seed);
                                }
                            });
                            password.wipe();
                            return;
                        }

                    } else {
                        if (!AddressManager.getInstance().hasHDMKeychain()) {
                            dp.setThread(null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dp.dismiss();
                                    DropdownMessage.showDropdownMessage(SignTxActivity.this, R
                                            .string.hdm_send_with_cold_no_requested_seed);
                                }
                            });
                            password.wipe();
                            return;
                        }
                        try {
                            DeterministicKey key = AddressManager.getInstance().getHdmKeychain()
                                    .getExternalKey(qrCodeTransport.getHdmIndex(), password);

                            List<String> hashes = qrCodeTransport.getHashList();
                            strings = new ArrayList<String>();
                            for (String hash : hashes) {
                                ECKey.ECDSASignature signed = key.sign(Utils.hexStringToByteArray
                                        (hash));
                                strings.add(Utils.bytesToHexString(signed.encodeToDER()));
                            }
                            key.wipe();
                        } catch (Exception e) {
                            e.printStackTrace();
                            dp.setThread(null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dp.dismiss();
                                    DropdownMessage.showDropdownMessage(SignTxActivity.this, R
                                            .string.hdm_send_with_cold_no_requested_seed);
                                }
                            });
                            password.wipe();
                            return;
                        }
                    }
                } else {
                    Address address = WalletUtils.findPrivateKey(qrCodeTransport.getMyAddress());
                    strings = address.signStrHashes(qrCodeTransport.getHashList(), password);
                }

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
                        Intent intent = new Intent(SignTxActivity.this, BitherQRCodeActivity.class);
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
