package net.bither.activity.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.api.BccBroadCastApi;
import net.bither.bitherj.api.BccHasAddressApi;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;
import net.bither.util.InputParser;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by ltq on 2017/7/29.
 */

public class SplitBccColdWalletSendActivity extends SplitBCCSendActivity {

    private long btcAmount;
    private String toAddress;
    public Tx tx;
    private boolean needConfirm = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        address = AddressManager.getInstance().getWatchOnlyAddresses().get(addressPosition);
        super.onCreate(savedInstanceState);
        etPassword.setVisibility(View.GONE);
        findViewById(R.id.tv_password).setVisibility(View.GONE);
        btnSend.setCompoundDrawablesWithIntrinsicBounds(R.drawable
                .unsigned_transaction_button_icon_mirror_transparent, 0, R.drawable
                .unsigned_transaction_button_icon, 0);
    }

    @Override
    protected void initAddress() {

    }

    protected void initBalance() {
        tvBalance.setText(UnitUtil.formatValue(getAmount(AbstractDb.txProvider.
                getUnspentOutputByBlockNo(BitherSetting.BTCFORKBLOCKNO, address.getAddress())),UnitUtil.BitcoinUnit.BTC));
    }

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
                    boolean result = jsonObject.getInt("result") == 1? true:false;
                    if (result) {
                        send();
                    } else {
                        DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,
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

    private DialogHdSendConfirm.SendConfirmListener sendConfirmListener = new DialogHdSendConfirm.SendConfirmListener() {

        @Override
        public void onConfirm() {
            String addressCannotBtParsed = getString(R.string.address_cannot_be_parsed);
            Intent intent = new Intent(SplitBccColdWalletSendActivity.this,
                    UnsignedTxQrCodeActivity.class);
            intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING,
                    QRCodeTxTransport.getPresignTxString(tx, toAddress, addressCannotBtParsed, QRCodeTxTransport.NO_HDM_INDEX));
            if (Utils.isEmpty(toAddress)) {
                intent.putExtra(BitherSetting.INTENT_REF.OLD_QR_CODE_STRING,
                        QRCodeTxTransport.oldGetPreSignString(tx, addressCannotBtParsed));
            } else {
                intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_HAS_CHANGE_ADDRESS_STRING
                        , true);
            }

            intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                    getString(R.string.unsigned_transaction_qr_code_title));
            startActivityForResult(intent, BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE);
            btnSend.setEnabled(true);
        }

        @Override
        public void onCancel() {

        }
    };

    private Handler completeTransactionHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_SUCCESS:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    if (msg.obj != null && msg.obj instanceof Tx) {
                        Tx smgTx = (Tx) msg.obj;
                        tx = smgTx;
                        tx.setBtc(false);
                        if (needConfirm) {
                            DialogHdSendConfirm dialog = new DialogHdSendConfirm
                                    (SplitBccColdWalletSendActivity.this, toAddress,tx, false,
                                            sendConfirmListener);
                            dialog.show();
                        } else {
                            sendConfirmListener.onConfirm();
                        }
                    } else {
                        DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,
                                R.string.password_wrong);
                    }
                    break;
                case HandlerMessage.MSG_PASSWORD_WRONG:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,
                            R.string.password_wrong);
                    break;
                case HandlerMessage.MSG_FAILURE:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    String msgError = getString(R.string.send_failed);
                    if (msg.obj instanceof String) {
                        msgError = (String) msg.obj;
                    }
                    DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this, msgError);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCommitTransactionSuccess(Tx tx) {
        needConfirm = true;
        if (dp.isShowing()) {
            dp.dismiss();
        }
        Intent intent = getIntent();
        if (tx != null) {
            intent.putExtra(SelectAddressToSendActivity.INTENT_EXTRA_TRANSACTION,
                    tx.getHashAsString());
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCommitTransactionFailed() {
        needConfirm = true;
        btnSend.setEnabled(true);
        DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this, R.string.send_failed);
    }

    private void send() {
        try {
            CompleteTransactionRunnable completeRunnable = new
                    CompleteTransactionRunnable(addressPosition, btcAmount,toAddress,toAddress,
                    null,false);
            completeRunnable.setHandler(completeTransactionHandler);
            Thread thread = new Thread(completeRunnable);
            dp.setThread(thread);
            if (!dp.isShowing()) {
                dp.show();
            }
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,
                    R.string.send_failed);
        }
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
                    String raw = Utils.bytesToHexString(tx.bitcoinSerialize());
                    BccBroadCastApi bccBroadCastApi = new BccBroadCastApi(raw);
                    bccBroadCastApi.handleHttpPost();
                    JSONObject jsonObject = new JSONObject(bccBroadCastApi.getResult());
                    boolean result = jsonObject.getInt("result") == 1 ?true:false;
                    if (result) {
                        saveIsObtainBcc();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,R.string.send_success);
                            }
                        });
                        success = true;
                    } else {
                        final JSONObject jsonObj = jsonObject.getJSONObject("error");
                        final int code = jsonObj.getInt("code");
                        final String message = jsonObj.getString("message");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,String.valueOf(code) + message);
                            }
                        });
                        success = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (success) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                        }
                    });
                    btnSend.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    },1000);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity
                                    .this, R.string.send_failed);
                            btnSend.setEnabled(true);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    protected void validateValues() {
        boolean isValidAmounts = false;
        btcAmount = getAmount(AbstractDb.txProvider.getUnspentOutputByBlockNo(BitherSetting.BTCFORKBLOCKNO, address.getAddress()));
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == Activity
                .RESULT_OK) {
            final String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            new InputParser.StringInputParser(input) {
                @Override
                protected void bitcoinRequest(final String address, final String addressLabel,
                                              final long amount, final String bluetoothMac) {
                    etAddress.setText(address.toString());
                    validateValues();
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs) {
                    DropdownMessage.showDropdownMessage(SplitBccColdWalletSendActivity.this,
                            R.string.scan_watch_only_address_error);
                }
            }.parse();
            return;
        }
        if (requestCode == BitherSetting.INTENT_REF.SIGN_TX_REQUEST_CODE && resultCode ==
                RESULT_OK) {
            final String qr = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            btnSend.setEnabled(false);
            btnSend.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean success;
                    String[] array = QRCodeUtil.splitString(qr);
                    ArrayList<byte[]> sigs = new ArrayList<byte[]>();
                    for (String s : array) {
                        sigs.add(Utils.hexStringToByteArray(replaceSignHashOfString(s)));
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
                                        (SplitBccColdWalletSendActivity.this, R.string
                                                .unsigned_transaction_sign_failed);
                                btnSend.setEnabled(true);
                            }
                        });
                    }
                }
            }, 500);
        }
    }

    private String replaceSignHashOfString(String s) {
        String endString = s.substring(s.length()-68,s.length());
        String appendString = "41"; // 1|0x40|0   Hex
        String startString = s.substring(0,s.length()-70);
        return startString+appendString+endString;
    }

    void saveIsObtainBcc() {
        AppSharedPreference.getInstance().setIsObtainBcc(address.getAddress(),true);
    }
}
