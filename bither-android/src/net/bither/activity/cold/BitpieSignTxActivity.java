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

package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.Coin;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.UnitUtilWrapper;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

//
// create by yiwenlong(wlong.yi@gmail.com) 2019/01/17
//

public class BitpieSignTxActivity extends SwipeRightFragmentActivity implements IDialogPasswordListener {

    private QRCodeTxTransport qrCodeTransport;

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

    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_bitpie_sign_tx);
        initView();
        toScanActivity();
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

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(
                new IBackClickListener(0, R.anim.slide_out_right));
        tvFrom = findViewById(R.id.tv_address_from);
        tvTo = findViewById(R.id.tv_address_to);
        tvAmount = findViewById(R.id.tv_amount);
        tvFee = findViewById(R.id.tv_fee);
        llChange = findViewById(R.id.ll_change);
        tvAddressChange = findViewById(R.id.tv_address_change);
        tvAmountChange = findViewById(R.id.tv_amount_change);
        tvSymbolChange = findViewById(R.id.tv_symbol_change);
        btnSign = findViewById(R.id.btn_sign);
        tvCannotFindPrivateKey = findViewById(R.id.tv_can_not_find_private_key);
        tvSymbol = findViewById(R.id.tv_symbol);
        tvFeeSymbol = findViewById(R.id.tv_fee_symbol);
        btnSign.setEnabled(false);
        btnSign.setOnClickListener(signClick);
        dp = new DialogProgress(this, R.string.signing_transaction);
    }

    private View.OnClickListener signClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogPassword dialogPassword = new DialogPassword(
                    BitpieSignTxActivity.this, BitpieSignTxActivity.this);
            dialogPassword.show();
        }
    };

    private void showTransaction() {
        String symbol;
        Coin coin = Utils.getCoinByAddressHeader(qrCodeTransport.getToAddress());
        if(coin == Coin.BTC) {
            symbol = AppSharedPreference.getInstance().getBitcoinUnit().name();
        } else {
            symbol = coin.getSplitCoin().getName();
        }

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
        tvAmount.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getTo(), coin));
        tvFee.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getFee(), coin));
        llChange.setVisibility(View.GONE);
        if(!Utils.isEmpty(qrCodeTransport.getChangeAddress())){
            llChange.setVisibility(View.VISIBLE);
            tvAddressChange.setText(WalletUtils.formatHash(qrCodeTransport.getChangeAddress(), 4, qrCodeTransport.getChangeAddress().length()));
            tvAmountChange.setText(UnitUtilWrapper.formatValueWithBold(qrCodeTransport.getChangeAmt(),coin));
        }
        Address address = WalletUtils
                .findPrivateKey(qrCodeTransport.getMyAddress());
        if ((qrCodeTransport.getHdmIndex() < 0 && address == null && qrCodeTransport
                .getTxTransportType() != QRCodeTxTransport.TxTransportType.ColdHD) ||
                (qrCodeTransport
                        .getHdmIndex() >= 0 && qrCodeTransport.getTxTransportType() != QRCodeTxTransport
                        .TxTransportType.ColdHDM && !AddressManager.getInstance().hasHDMKeychain()) ||
                (qrCodeTransport.getHdmIndex() >= 0 && qrCodeTransport.getTxTransportType() ==
                        QRCodeTxTransport.TxTransportType.ColdHDM && !EnterpriseHDMSeed.hasSeed()
                ) || (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType
                .ColdHD && !AddressManager.getInstance().hasHDAccountCold())) {
            btnSign.setEnabled(false);
            tvCannotFindPrivateKey.setVisibility(View.VISIBLE);
        } else {
            btnSign.setEnabled(true);
            tvCannotFindPrivateKey.setVisibility(View.GONE);
        }
    }


    private void toScanActivity() {
        Intent intent = new Intent(BitpieSignTxActivity.this,
                ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.bitpie_scan_unsigned_transaction_title));
        startActivityForResult(intent,
                BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
    }

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        Thread thread = new Thread() {
            public void run() {
                List<String> strings = null;
                if (qrCodeTransport.getTxTransportType() == QRCodeTxTransport.TxTransportType.ColdHD) {
                    BitpieHDAccountCold account = AddressManager.getInstance().getBitpieHDAccountCold();
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
                                DropdownMessage.showDropdownMessage(BitpieSignTxActivity.this, R.string
                                        .unsigned_transaction_sign_failed);
                            }
                        });
                        password.wipe();
                        return;
                    }
                } else {
                    dp.setThread(null);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(BitpieSignTxActivity.this, R.string
                                    .unsigned_transaction_sign_failed);
                        }
                    });
                    password.wipe();
                    return;
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
                        Intent intent = new Intent(BitpieSignTxActivity.this, BitherQRCodeActivity.class);
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
}
