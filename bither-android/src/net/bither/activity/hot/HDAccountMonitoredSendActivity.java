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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.SendActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccountMonitored;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.KeyCrypterException;
import net.bither.bitherj.crypto.TransactionSignature;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/4/17.
 */
public class HDAccountMonitoredSendActivity extends SendActivity implements DialogHdSendConfirm
        .SendConfirmListener {
    private long btcAmount;
    private String toAddress;
    private Tx tx;

    static {
        CompleteTransactionRunnable.registerTxBuilderExceptionMessages();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.ibtn_option).setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        findViewById(R.id.tv_password).setVisibility(View.GONE);
        btnSend.setCompoundDrawablesWithIntrinsicBounds(R.drawable
                .unsigned_transaction_button_icon_mirror_transparent, 0, R.drawable
                .unsigned_transaction_button_icon, 0);
    }

    @Override
    protected void initAddress() {
        address = AddressManager.getInstance().getHdAccountMonitored();
        addressPosition = 0;
    }

    @Override
    protected void sendClicked() {
        final long btc = amountCalculatorLink.getAmount();
        if (btc > 0) {
            btcAmount = btc;
            tx = null;
            String address = etAddress.getText().toString().trim();
            if (Utils.validBicoinAddress(address)) {
                toAddress = address;
                if (!dp.isShowing()) {
                    dp.show();
                }
                new Thread() {
                    @Override
                    public void run() {
                        send();
                    }
                }.start();
            } else {
                DropdownMessage.showDropdownMessage(HDAccountMonitoredSendActivity.this, R.string
                        .send_failed);
            }
        }
    }

    private void send() {
        tx = null;
        HDAccountMonitored account = (HDAccountMonitored) address;
        try {
            tx = account.newTx(toAddress, btcAmount);
        } catch (Exception e) {
            e.printStackTrace();
            btcAmount = 0;
            tx = null;
            String msg = getString(R.string.send_failed);
            if (e instanceof KeyCrypterException || e instanceof MnemonicException
                    .MnemonicLengthException) {
                msg = getString(R.string.password_wrong);
            } else if (e instanceof TxBuilderException) {
                msg = e.getMessage();
            }
            final String m = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(HDAccountMonitoredSendActivity.this, m);
                }
            });
        }
        if (tx != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showConfirm();
                }
            });
        }
    }

    private void showConfirm() {
        if (dp.isShowing()) {
            dp.dismiss();
        }
        new DialogHdSendConfirm(this, toAddress, tx, this).show();
    }

    @Override
    public void onConfirm() {
        Intent intent = new Intent(this, UnsignedTxQrCodeActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, QRCodeTxTransport
                .getHDAccountMonitoredUnsignedTx(tx, toAddress, (HDAccountMonitored) address));
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string
                .unsigned_transaction_qr_code_title));
        startActivityForResult(intent, BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE);
        btnSend.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                btnSend.setEnabled(false);
                final String qr = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                if (!dp.isShowing()) {
                    dp.show();
                }
                new Thread() {
                    @Override
                    public void run() {
                        String[] array = QRCodeUtil.splitString(qr);
                        ArrayList<byte[]> sigs = new ArrayList<byte[]>();
                        for (String s : array) {
                            ECKey.ECDSASignature sig = ECKey.ECDSASignature.decodeFromDER(Utils
                                    .hexStringToByteArray(s));
                            sigs.add(new TransactionSignature(sig, TransactionSignature.SigHash
                                    .ALL, false).encodeToBitcoin());
                        }
                        tx.signWithSignatures(sigs);
                        if (tx.verifySignatures()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendTx();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    DropdownMessage.showDropdownMessage
                                            (HDAccountMonitoredSendActivity.this, R.string
                                                    .unsigned_transaction_sign_failed);
                                    btnSend.setEnabled(true);
                                }
                            });
                        }
                    }
                }.start();
            } else {
                btnSend.setEnabled(true);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendTx() {
        if (!dp.isShowing()) {
            dp.show();
        }
        new Thread() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    PeerManager.instance().publishTransaction(tx);
                    success = true;
                    tx = null;
                    toAddress = null;
                    btcAmount = 0;
                } catch (PeerManager.PublishUnsignedTxException e) {
                    e.printStackTrace();
                    tx = null;
                    toAddress = null;
                    btcAmount = 0;
                }
                if (success) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            Intent intent = getIntent();
                            if (tx != null) {
                                intent.putExtra(SelectAddressToSendActivity
                                        .INTENT_EXTRA_TRANSACTION, tx.getHashAsString());
                            }
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(HDAccountMonitoredSendActivity
                                    .this, R.string.send_failed);
                            btnSend.setEnabled(true);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void onCancel() {
        tx = null;
        toAddress = null;
        btcAmount = 0;
    }
}
