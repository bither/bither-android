package net.bither;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import net.bither.activity.hot.UnsignedTxQrCodeActivity;
import net.bither.bitherj.api.BccBroadCastApi;
import net.bither.bitherj.api.BccHasAddressApi;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.model.ExtractBccUtxo;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogHdSendConfirm;
import net.bither.util.InputParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ltq on 2017/9/20.
 */

public class BCCAssetsHotMonitoredActivity extends BCCAssetsDetectHotActivity {
    private long btcAmount;
    private String toAddress;
    public List<Tx> txs;
    private List<Out> outs;
    private boolean needConfirm = true;
    private int kSignTypeLength = 2;
    private int kCompressPubKeyLength = 68;
    private int kUncompressedPubKeyLength = 132;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initAddress();
        super.onCreate(savedInstanceState);
        etPassword.setVisibility(View.GONE);
        findViewById(R.id.tv_password).setVisibility(View.GONE);
        btnSend.setCompoundDrawablesWithIntrinsicBounds(R.drawable
                .unsigned_transaction_button_icon_mirror_transparent, 0, R.drawable
                .unsigned_transaction_button_icon, 0);
    }

    @Override
    protected void initAddress() {
        addressPosition = getIntent().getExtras().getInt(BitherSetting.INTENT_REF
                .ADDRESS_POSITION_PASS_VALUE_TAG);
        if (addressPosition >= 0 && AddressManager.getInstance().getWatchOnlyAddresses()
                != null && addressPosition < AddressManager.getInstance()
                .getWatchOnlyAddresses().size()) {
            address = AddressManager.getInstance().getWatchOnlyAddresses().get
                    (addressPosition);
        }
    }

    protected void initBalance() {
        List<ExtractBccUtxo> extractBccUtxos = (List<ExtractBccUtxo>) getIntent().getExtras().getSerializable(DECTECTED_BCC_AMOUNT_TAG);
        outs = ExtractBccUtxo.rawOutList(extractBccUtxos);
        tvBalance.setText(UnitUtil.formatValue(getAmount(outs),UnitUtil.BitcoinUnit.BTC));
        btcAmount = getAmount(outs);
    }

    protected void sendClicked() {
        if (!dp.isShowing()) {
            dp.show();
        }
        BaseRunnable baseRunnable = new BaseRunnable() {
            @Override
            public void run() {
                BccHasAddressApi bccHasAddressApi = new BccHasAddressApi(toAddress, SplitCoin.BCC);
                try {
                    bccHasAddressApi.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(bccHasAddressApi
                            .getResult());
                    boolean result = jsonObject.getInt("result") == 1? true:false;
                    if (result) {
                        send();
                    } else {
                        DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this,
                                Utils.format(getString(R.string.not_bitpie_split_coin_address), SplitCoin.BCC.getName()));
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
            Intent intent = new Intent(BCCAssetsHotMonitoredActivity.this,
                    UnsignedTxQrCodeActivity.class);

            intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING,
                    QRCodeTxTransport.getBccPresignTxString(txs, toAddress,address.getAddress(), addressCannotBtParsed,outs));
            if (Utils.isEmpty(toAddress)) {
                intent.putExtra(BitherSetting.INTENT_REF.OLD_QR_CODE_STRING,
                        QRCodeTxTransport.oldGetBccPreSignString(txs, addressCannotBtParsed));
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
                    if (msg.obj != null && msg.obj instanceof List) {
                        List<Tx> smgTx = (List<Tx>) msg.obj;
                        txs = smgTx;
                        if (needConfirm) {
                            new DialogHdSendConfirm(BCCAssetsHotMonitoredActivity.this, toAddress, txs,Utils.getFeeBase(), sendConfirmListener).show();
                        } else {
                            sendConfirmListener.onConfirm();
                        }
                    } else {
                        DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this,
                                R.string.password_wrong);
                    }
                    break;
                case HandlerMessage.MSG_PASSWORD_WRONG:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this,
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
                    DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this, msgError);
                    break;
                default:
                    break;
            }
        }
    };

    private void send() {
        try {
            CompleteTransactionRunnable completeRunnable = new
                    CompleteTransactionRunnable(addressPosition, btcAmount, toAddress, toAddress,
                    null,false,outs);
            completeRunnable.setHandler(completeTransactionHandler);
            Thread thread = new Thread(completeRunnable);
            dp.setThread(thread);
            if (!dp.isShowing()) {
                dp.show();
            }
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this,
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
                String errorMsg = null;
                for (final Tx tx: txs) {
                    try {
                        String raw = Utils.bytesToHexString(tx.bitcoinSerialize());
                        BccBroadCastApi bccBroadCastApi = new BccBroadCastApi(raw,SplitCoin.BCC);
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
                            DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this, R.string.send_success);
                        }
                    });
                    btnSend.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            setResult(SplitBccSelectAddressActivity.
//                                    SPLIT_BCC_HDACCOUNT_REQUEST_CODE, null);
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
                            DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this, finalErrorMsg);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == Activity
                .RESULT_OK) {
            final String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            new InputParser.StringInputParser(input, SplitCoin.BCC) {
                @Override
                protected void bitcoinRequest(final String address, final String addressLabel,
                                              final long amount, final String bluetoothMac) {
                    etAddress.setText(address.toString());
                    validateValues();
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs) {
                    DropdownMessage.showDropdownMessage(BCCAssetsHotMonitoredActivity.this,
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
                            ArrayList<byte[]> compressSigs = new ArrayList<byte[]>();
                            ArrayList<byte[]> uncompressedSigs = new ArrayList<byte[]>();
                            for (int j = 0; j < tx.getIns().size(); j++) {
                                String s = array[strIndex + j];
                                compressSigs.add(Utils.hexStringToByteArray(replaceSignHashOfString(s, kCompressPubKeyLength)));
                                uncompressedSigs.add(Utils.hexStringToByteArray(replaceSignHashOfString(s, kUncompressedPubKeyLength)));
                            }
                            tx.setDetectBcc(true);
                            tx.signWithSignatures(compressSigs);
                            if (!tx.verifySignatures()) {
                                tx.signWithSignatures(uncompressedSigs);
                            }
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
                                        (BCCAssetsHotMonitoredActivity.this, R.string
                                                .unsigned_transaction_sign_failed);
                                btnSend.setEnabled(true);
                            }
                        });
                    }
                }
            }, 500);
        }
    }

    private String replaceSignHashOfString(String s, int pubKeyLength) {
        if (s.length() > pubKeyLength + kSignTypeLength) {
            String endString = s.substring(s.length() - pubKeyLength, s.length());
            String appendString = "41"; // 1|0x40|0   Hex
            String startString = s.substring(0, s.length()- pubKeyLength - kSignTypeLength);
            return startString + appendString + endString;
        } else {
            return s;
        }
    }
}
