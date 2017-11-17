package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.api.BccBroadCastApi;
import net.bither.bitherj.api.BccHasAddressApi;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.KeyCrypterException;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;

import org.json.JSONObject;

import java.util.List;

import static net.bither.activity.hot.HotAdvanceActivity.SplitCoinKey;

/**
 * Created by ltq on 2017/7/28.
 */

public class SplitBCCHDAccountSendActivity extends SplitBCCSendActivity implements DialogHdSendConfirm
        .SendConfirmListener {

    private long btcAmount;
    private String toAddress;
    private List<Tx> txs;

    static {
        CompleteTransactionRunnable.registerTxBuilderExceptionMessages();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        splitCoin = (SplitCoin) intent.getSerializableExtra(SplitCoinKey);
    }

    @Override
    protected void initAddress() {
            address = AddressManager.getInstance().getHDAccountHot();
            addressPosition = 0;
    }

    @Override
    protected void initBalance() {
            tvBalance.setText(UnitUtil.formatValue(getAmount(AbstractDb.hdAccountAddressProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(),
                    AddressManager.getInstance().getHDAccountHot().getHdSeedId())),UnitUtil.BitcoinUnit.BTC));
    }

    @Override
    protected void sendClicked() {
        if (!dp.isShowing()) {
            dp.show();
        }
        BaseRunnable baseRunnable = new BaseRunnable() {
            @Override
            public void run() {
                BccHasAddressApi bccHasAddressApi = new BccHasAddressApi(toAddress, splitCoin);
                try {
                    bccHasAddressApi.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(bccHasAddressApi
                            .getResult());
                    boolean result = jsonObject.getInt("result") == 1 ? true : false;
                    if (result) {
                        send();
                    } else {
                        DropdownMessage.showDropdownMessage(SplitBCCHDAccountSendActivity.this,
                                Utils.format(getString(R.string.not_bitpie_split_coin_address), splitCoin.getName()));
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(baseRunnable).start();
    }

    private void send() {
        txs = null;
        HDAccount account = (HDAccount) address;
        SecureCharSequence password = new SecureCharSequence(etPassword.getText());
        try {
            txs = account.newForkTx(etAddress.getText().toString().trim(), btcAmount, password, splitCoin);
        } catch (Exception e) {
            e.printStackTrace();
            btcAmount = 0;
            txs = null;
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
                    DropdownMessage.showDropdownMessage(SplitBCCHDAccountSendActivity.this, m);
                }
            });
        } finally {
            password.wipe();
        }
        if (txs != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showConfirm();

                }
            });
        }
    }

    @Override
    protected void validateValues() {
        boolean isValidAmounts = false;
            btcAmount = getAmount(AbstractDb.hdAccountAddressProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(),
                    AddressManager.getInstance().getHDAccountHot().getHdSeedId()));
        if (btcAmount > 0) {
            isValidAmounts = true;
        }
        toAddress = etAddress.getText().toString().trim();
        boolean isValidAddress = Utils.validBicoinAddress(toAddress) || Utils.validBicoinGoldAddress(toAddress);
        boolean isValidPassword = true;
        if (etPassword.getVisibility() == View.VISIBLE) {
            SecureCharSequence password = new SecureCharSequence(etPassword.getText());
            isValidPassword = Utils.validPassword(password) && password.length() >= 6 &&
                    password.length() <= getResources().getInteger(R.integer.password_length_max);
            password.wipe();
        }
        btnSend.setEnabled(isValidAddress && isValidAmounts && isValidPassword);
    }

    private void showConfirm() {
        if (dp.isShowing()) {
            dp.dismiss();
        }
        new DialogHdSendConfirm(this, toAddress, txs, this, splitCoin).show();
    }

    @Override
    public void onConfirm() {
        if (!dp.isShowing()) {
            dp.show();
        }

        new Thread() {
            @Override
            public void run() {
                String errorMsg = null;
                for (final Tx tx: txs) {
                    try {
                        String raw = Utils.bytesToHexString(tx.bitcoinSerialize());
                        BccBroadCastApi bccBroadCastApi = new BccBroadCastApi(raw, splitCoin);
                        bccBroadCastApi.handleHttpPost();
                        JSONObject jsonObject = new JSONObject(bccBroadCastApi.getResult());
                        boolean result = jsonObject.getInt("result") == 1 ? true : false;
                        if (!result) {
                            final JSONObject jsonObj = jsonObject.getJSONObject("error");
                            final int code = jsonObj.getInt("code");
                            final String message = jsonObj.getString("message");
                            errorMsg = String.valueOf(code) + message;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMsg = getString(R.string.send_failed);
                    }

                    if (errorMsg != null) {
                        break;
                    }
                }

                if (errorMsg == null) {
                    saveIsObtainBcc();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(SplitBCCHDAccountSendActivity.this, R.string.send_success);
                        }
                    });
                    btnSend.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setResult(SplitBccSelectAddressActivity.
                                    SPLIT_BCC_HDACCOUNT_REQUEST_CODE, null);
                            finish();
                        }
                    },1000);
                } else {
                    final String finalErrorMsg = errorMsg;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(SplitBCCHDAccountSendActivity.this, finalErrorMsg);
                            btnSend.setEnabled(true);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void onCancel() {

    }

    void saveIsObtainBcc() {
        AppSharedPreference.getInstance().setIsObtainBcc("HDAccountHot" + splitCoin.getIsGatKey(), true);
    }
}
