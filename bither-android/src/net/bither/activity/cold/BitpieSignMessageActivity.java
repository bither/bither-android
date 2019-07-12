package net.bither.activity.cold;

import android.content.Intent;
import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.qrcode.ScanQRCodeTransportActivity;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IDialogPasswordListener;

public class BitpieSignMessageActivity extends SwipeRightFragmentActivity implements IDialogPasswordListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toScanActivity();
    }

    private void toScanActivity() {
        Intent intent = new Intent(BitpieSignMessageActivity.this,
                ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                getString(R.string.bitpie_scan_message_title));
        startActivityForResult(intent,
                BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
    }

    @Override
    public void onPasswordEntered(SecureCharSequence password) {

    }
}
