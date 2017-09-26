package net.bither;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.bither.activity.hot.SplitBccSelectAddressActivity;
import net.bither.activity.hot.UnsignedTxQrCodeActivity;
import net.bither.bitherj.api.BccBroadCastApi;
import net.bither.bitherj.api.BccHasAddressApi;
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.KeyCrypterException;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.exception.TxBuilderException;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.model.ExtractBccUtxo;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ltq on 2017/9/20.
 */

public class BCCAssetsHDAccountMonitoredActivity extends BCCAssetsDetectHotActivity implements DialogHdSendConfirm
        .SendConfirmListener {
    private long btcAmount;
    private String toAddress;
    private List<Tx> txs;
    private List<Out> outs;
    private AbstractHD.PathType path;
    private int index;
    private String myAddress;

    static {
        CompleteTransactionRunnable.registerTxBuilderExceptionMessages();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        etPassword.setVisibility(View.GONE);
        findViewById(R.id.tv_password).setVisibility(View.GONE);
        btnSend.setCompoundDrawablesWithIntrinsicBounds(R.drawable
                .unsigned_transaction_button_icon_mirror_transparent, 0, R.drawable
                .unsigned_transaction_button_icon, 0);
    }

    @Override
    protected void initAddress() {
        address = AddressManager.getInstance().getHDAccountMonitored();
        addressPosition = 0;
    }

    @Override
    protected void initBalance() {
        List<ExtractBccUtxo> extractBccUtxos = (List<ExtractBccUtxo>) getIntent().getExtras().getSerializable(DECTECTED_BCC_AMOUNT_TAG);
        outs = ExtractBccUtxo.rawOutList(extractBccUtxos);
        tvBalance.setText(UnitUtil.formatValue(getAmount(outs),UnitUtil.BitcoinUnit.BTC));
        path = AbstractHD.getTernalRootType(getIntent().getExtras().getInt(DECTECTED_BCC_HD_PATH_TYPE));
        index = getIntent().getExtras().getInt(DECTECTED_BCC_HD_ADDRESS_INDEX);
        myAddress = getIntent().getStringExtra(DETECT_BCC_ADDRESS);
        btcAmount = getAmount(outs);
    }

    @Override
    protected void sendClicked() {
        if (!dp.isShowing()) {
            dp.show();
        }
        BaseRunnable baseRunnable = new BaseRunnable() {
            @Override
            public void run() {
                BccHasAddressApi bccHasAddressApi = new BccHasAddressApi(toAddress);
                try {
                    bccHasAddressApi.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(bccHasAddressApi
                            .getResult());
                    boolean result = jsonObject.getInt("result") == 1 ? true : false;
                    if (result) {
                        if (!dp.isShowing()) {
                            dp.show();
                        }
                        send();
                    } else {
                        DropdownMessage.showDropdownMessage(BCCAssetsHDAccountMonitoredActivity.this,
                                getString(R.string.not_bitpie_bcc_address));
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
        this.txs = null;
        HDAccount account = (HDAccount) address;
        try {
            txs = account.newForkTx(toAddress, btcAmount,outs);
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
                    DropdownMessage.showDropdownMessage(BCCAssetsHDAccountMonitoredActivity.this, m);
                }
            });
        }

        if (this.txs != null) {
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
        btcAmount = getAmount(outs);
        if (btcAmount > 0) {
            isValidAmounts = true;
        }
        toAddress = etAddress.getText().toString().trim();
        boolean isValidAddress = Utils.validBicoinAddress(toAddress);
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
        new DialogHdSendConfirm(this, toAddress, txs, Utils.getFeeBase(), this).show();
    }

    @Override
    public void onConfirm() {
        Intent intent = new Intent(this, UnsignedTxQrCodeActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, QRCodeTxTransport.
                getBccHDAccountMonitoredUnsignedTx(txs, toAddress, myAddress,
                        (HDAccount) address, path, index, outs));
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
                        int insCount = 0;
                        for (Tx tx : txs) {
                            insCount += tx.getIns().size();
                        }
                        boolean success = insCount == array.length;
                        if (success) {
                            int strIndex = 0;
                            for (int i = 0; i < txs.size(); i++) {
                                Tx tx = txs.get(i);
                                tx.setDetectBcc(true);
                                ArrayList<byte[]> sigs = new ArrayList<byte[]>();
                                for (int j = 0; j < tx.getIns().size(); j++) {
                                    String s = array[strIndex + j];
                                    sigs.add(Utils.hexStringToByteArray(replaceSignHashOfString(s)));
                                }
                                tx.signWithSignatures(sigs);
                                if (!tx.verifySignatures()) {
                                    success = false;
                                    break;
                                }
                                strIndex += tx.getIns().size();
                            }
                        }

                        if (success) {
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
                                            (BCCAssetsHDAccountMonitoredActivity.this, R.string
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
                String errorMsg = null;
                for (final Tx tx: txs) {
                    try {
                        String raw = Utils.bytesToHexString(tx.bitcoinSerialize());
                        BccBroadCastApi bccBroadCastApi = new BccBroadCastApi(raw);
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(BCCAssetsHDAccountMonitoredActivity.this, R.string.send_success);
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
                            DropdownMessage.showDropdownMessage(BCCAssetsHDAccountMonitoredActivity.this, finalErrorMsg);
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

    private String replaceSignHashOfString(String s) {
        String endString = s.substring(s.length()-68,s.length());
        String appendString = "41"; // 1|0x40|0   Hex
        String startString = s.substring(0,s.length()-70);
        return startString+appendString+endString;
    }

}

