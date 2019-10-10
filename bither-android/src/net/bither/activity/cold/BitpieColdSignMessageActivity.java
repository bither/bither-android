package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.qrcode.QRCodeBitpieColdSignMessage;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;

public class BitpieColdSignMessageActivity extends SwipeRightActivity implements IDialogPasswordListener {

    private DialogProgress dp;
    private QRCodeBitpieColdSignMessage qrCodeBitpieColdSignMessage;

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
                final String result = qrCodeBitpieColdSignMessage.getBitherQrCodeStr(password);
                if (Utils.isEmpty(result)) {
                    dp.setThread(null);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(BitpieColdSignMessageActivity.this, R.string.unsigned_transaction_sign_failed);
                        }
                    });
                    return;
                }
                dp.setThread(null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dp.dismiss();
                        Intent intent = new Intent(BitpieColdSignMessageActivity.this, BitherQRCodeActivity.class);
                        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, result);
                        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string.bitpie_signed_message_qr_code_title));
                        intent.putExtra(BitherSetting.INTENT_REF.BITPIE_COLD_SIGN_MESSAGE_TYPE_STRING, qrCodeBitpieColdSignMessage.getSignMessageType().getType());
                        startActivity(intent);
                        finish();
                    }
                });
            }
        };
        dp.setThread(thread);
        thread.start();
        dp.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String str = data.getExtras().getString(ScanActivity.INTENT_EXTRA_RESULT);
            try {
                qrCodeBitpieColdSignMessage = QRCodeBitpieColdSignMessage.formatQRCode(str);
                if (qrCodeBitpieColdSignMessage != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            DialogPassword dialogPassword = new DialogPassword(BitpieColdSignMessageActivity.this, BitpieColdSignMessageActivity.this);
                            dialogPassword.show();
                        }
                    }, 400);
                } else {
                    super.finish();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                super.finish();
            }
        } else {
            super.finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

}
