/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.zxing.Result;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.util.PlaySound;

public class ScanQRCodeWithOtherActivity extends ScanActivity {
    private TextView tv;
    private TextView tvTitle;
    private BitherSetting.QRCodeType qrCodeType;
    private String lastResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().hasExtra(BitherSetting.INTENT_REF.QRCODE_TYPE)) {
            qrCodeType = (BitherSetting.QRCodeType)
                    getIntent().getSerializableExtra(BitherSetting.INTENT_REF.QRCODE_TYPE);
        }
        if (qrCodeType == null) {
            finish();
        }
        setOverlay(R.layout.layout_scan_qr_code_transport_overlay);

        tv = (TextView) findViewById(R.id.tv);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        configureTitle();
        flash();
        PlaySound.loadSound(R.raw.qr_code_scanned);
    }

    @Override
    public void handleResult(Result scanResult, Bitmap thumbnailImage,
                             float thumbnailScaleFactor) {
        String result = scanResult.getText();
        PlaySound.play(R.raw.qr_code_scanned, null);
        complete(result);
    }

    private void complete(String string) {
        try {
            Intent intent = getIntent();
            intent.putExtra(INTENT_EXTRA_RESULT, string);
            setResult(RESULT_OK, getIntent());
        } catch (Exception e) {
            e.printStackTrace();
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public boolean resultValid(String result) {
        boolean isValid = qrCodeType.checkFormat(result);
        if (!isValid) {
            if (!Utils.compareString(lastResult, result)) {
                shake();
                lastResult = result;
            }
        }
        return isValid;
    }

    private void configureTitle() {
        String title = null;
        if (getIntent() != null
                && getIntent().getExtras() != null
                && getIntent().getExtras().containsKey(
                BitherSetting.INTENT_REF.TITLE_STRING)) {
            title = getIntent().getExtras().getString(
                    BitherSetting.INTENT_REF.TITLE_STRING);
        }
        tvTitle.setText(title);
    }

    private void flash() {
        tv.clearAnimation();
        tv.startAnimation(AnimationUtils.loadAnimation(this,
                R.anim.scanner_label_flash));
    }

    private void shake() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vibrate();
                tv.clearAnimation();
                Animation anim = AnimationUtils.loadAnimation(
                        ScanQRCodeWithOtherActivity.this,
                        R.anim.password_wrong_warning);
                anim.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        flash();
                    }
                });
                tv.startAnimation(anim);
            }
        });
    }


}
