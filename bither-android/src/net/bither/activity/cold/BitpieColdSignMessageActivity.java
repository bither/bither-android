package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.hd.DeterministicKey;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

import static net.bither.bitherj.qrcode.QRCodeUtil.BITPIE_COLD_MONITOR_QR_SING_MESSAGE_PREFIX;

public class BitpieColdSignMessageActivity extends SwipeRightActivity implements IDialogPasswordListener {

    private DialogProgress dp;
    private String scanResult;
    private BitpieHDAccountCold bitpieHDAccountCold = AddressManager.getInstance().getBitpieHDAccountCold();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dp = new DialogProgress(this, R.string.signing_transaction);
        toScanActivity();
    }

    private void toScanActivity() {
        Intent intent = new Intent(BitpieColdSignMessageActivity.this,
                ScanActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.bitpie_scan_message_title));
        startActivityForResult(intent,
                BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
    }

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        Thread thread = new Thread() {
            public void run() {
                List<String> strings = new ArrayList<>();
                DeterministicKey normalKey = bitpieHDAccountCold.getExternalKey(0, password);
                DeterministicKey segwitKey = bitpieHDAccountCold.getSegwitExternalKey(0, password);
                strings.add(normalKey.signMessage(scanResult));
                strings.add(segwitKey.signMessage(scanResult));
                password.wipe();
                String result = BITPIE_COLD_MONITOR_QR_SING_MESSAGE_PREFIX;
                for (int i = 0; i < strings.size(); i++) {
                    if (i < strings.size() - 1) {
                        result = result + strings.get(i) + QRCodeUtil.HD_MONITOR_QR_SPLIT;
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
                        Intent intent = new Intent(BitpieColdSignMessageActivity.this, BitherQRCodeActivity.class);
                        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, r);
                        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string.bitpie_signed_message_qr_code_title));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            String str = data.getExtras().getString(ScanActivity.INTENT_EXTRA_RESULT);
            if (str != null) {
                if (str.startsWith("bitid://bitpie.com/password/")) {
                    this.scanResult = str;
                    DialogPassword dialogPassword = new DialogPassword(this, this);
                    dialogPassword.show();
                } else {
                    ThreadUtil.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            DropdownMessage.showDropdownMessage(BitpieColdSignMessageActivity.this, "请扫比特派钱包上的待签名消息");
                            BitpieColdSignMessageActivity.super.finish();
                        }
                    }, 500);
                }
            } else {
                super.finish();
            }
        } else {
            super.finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

}
