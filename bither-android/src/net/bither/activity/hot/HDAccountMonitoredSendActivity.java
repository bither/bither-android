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

import net.bither.R;
import net.bither.SendActivity;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.KeyCrypterException;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.utils.Utils;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;

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
    }

    @Override
    protected void initAddress() {
        address = AddressManager.getInstance().getHdAccount();
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
        HDAccount account = (HDAccount) address;
        SecureCharSequence password = new SecureCharSequence(etPassword.getText());
        try {
            tx = account.newTx(toAddress, btcAmount, password);
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
        } finally {
            password.wipe();
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
                            DropdownMessage.showDropdownMessage(HDAccountMonitoredSendActivity.this, R
                                    .string.send_failed);
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
