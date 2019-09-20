package net.bither.activity.cold;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.exception.BitpieColdNoSupportCoinException;
import net.bither.bitherj.qrcode.QRCodeBitpieColdSignMessage;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.BitherQRCodeActivity;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;

public class BitpieColdSignChangeCoinActivity extends SwipeRightActivity implements IDialogPasswordListener {

    private TextView tvCoinType;
    private Button btnSign;
    private TextView tvCannotFindPrivateKey;

    private DialogProgress dp;
    private QRCodeBitpieColdSignMessage qrCodeBitpieColdSignMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitpie_cold_sign_change_coin);
        toScanActivity();
        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String str = data.getExtras().getString(ScanActivity.INTENT_EXTRA_RESULT);
            try {
                QRCodeBitpieColdSignMessage signMessage = QRCodeBitpieColdSignMessage.formatQRCode(str);
                if (signMessage == null) {
                    super.finish();
                    return;
                } else {
                    qrCodeBitpieColdSignMessage = signMessage;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showChangeCoin();
                        }
                    }, 400);
                }
            } catch (BitpieColdNoSupportCoinException ex) {
                ex.printStackTrace();
                showScanResultInvalid(getString(R.string.bitpie_no_support_coin));
            } catch (Exception ex) {
                ex.printStackTrace();
                super.finish();
                return;
            }
        } else {
            super.finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener(0, R.anim.slide_out_right));
        tvCoinType = findViewById(R.id.tv_coin_type);
        btnSign = findViewById(R.id.btn_sign);
        tvCannotFindPrivateKey = findViewById(R.id.tv_can_not_find_private_key);
        btnSign.setEnabled(false);
        btnSign.setOnClickListener(signClick);
        dp = new DialogProgress(this, R.string.signing_transaction);
    }

    private View.OnClickListener signClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogPassword dialogPassword = new DialogPassword(BitpieColdSignChangeCoinActivity.this, BitpieColdSignChangeCoinActivity.this);
            dialogPassword.show();
        }
    };

    private void showChangeCoin() {
        tvCoinType.setText(qrCodeBitpieColdSignMessage.getCoinDisplayCode());
        if (!AddressManager.getInstance().hasBitpieHDAccountCold() || !qrCodeBitpieColdSignMessage.getBtcFirstAddress().equals(AddressManager.getInstance().getBitpieHDAccountCold().getFirstAddressFromDb())) {
            btnSign.setEnabled(false);
            tvCannotFindPrivateKey.setVisibility(View.VISIBLE);
        } else {
            btnSign.setEnabled(true);
            if (qrCodeBitpieColdSignMessage.getSignMessageType() == QRCodeBitpieColdSignMessage.SignMessageType.ChangeCoinSign) {
                btnSign.setText(R.string.bitpie_signed_message_change_coin);
            }
            tvCannotFindPrivateKey.setVisibility(View.GONE);
        }
    }

    private void toScanActivity() {
        Intent intent = new Intent(BitpieColdSignChangeCoinActivity.this, ScanActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string.bitpie_scan_message_title));
        startActivityForResult(intent, BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
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
                            DropdownMessage.showDropdownMessage(BitpieColdSignChangeCoinActivity.this, R.string.unsigned_transaction_sign_failed);
                        }
                    });
                    return;
                }
                dp.setThread(null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dp.dismiss();
                        Intent intent = new Intent(BitpieColdSignChangeCoinActivity.this, BitherQRCodeActivity.class);
                        intent.putExtra(BitherSetting.INTENT_REF.QR_CODE_STRING, result);
                        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING, getString(R.string.bitpie_signed_change_coin_qr_code_title));
                        intent.putExtra(BitherSetting.INTENT_REF.BITPIE_COLD_SIGN_MESSAGE_TYPE_STRING, qrCodeBitpieColdSignMessage.getSignMessageType().getType());
                        intent.putExtra(BitherSetting.INTENT_REF.BITPIE_COLD_CHANGE_COIN_IS_ONLY_GET_XPUB_STRING, qrCodeBitpieColdSignMessage.isOnlyGetXpub());
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
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}
