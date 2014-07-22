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

package net.bither.ui.base;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.params.MainNetParams;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.model.BitherAddress;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.PasswordSeed;
import net.bither.util.PrivateKeyUtil;

import org.spongycastle.crypto.params.KeyParameter;

import java.security.interfaces.ECKey;

public class DialogAddressWithShowPrivateKey extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener, DialogPassword.DialogPasswordListener {
    private DialogFancyQrCode dialogQr;
    private DialogPrivateKeyQrCode dialogPrivateKey;
    private BitherAddress address;
    private LinearLayout llOriginQRCode;
    private boolean showPrivateText = false;
    private Activity activity;
    private int clickedView;

    public DialogAddressWithShowPrivateKey(Activity context,
                                           BitherAddress address) {
        super(context);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_show_private_key);
        llOriginQRCode = (LinearLayout) findViewById(R.id.ll_origin_qr_code);
        findViewById(R.id.tv_view_show_private_key).setOnClickListener(this);
        findViewById(R.id.tv_private_key_qr_code).setOnClickListener(this);
        llOriginQRCode.setOnClickListener(this);
        llOriginQRCode.setVisibility(View.GONE);
        findViewById(R.id.tv_close).setOnClickListener(this);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
        dialogPrivateKey = new DialogPrivateKeyQrCode(context, address.getKeys().get(0));
    }

    @Override
    public void show() {
        clickedView = 0;
        super.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (clickedView) {
            case R.id.ll_origin_qr_code:
                dialogQr.show();
                break;
            case R.id.tv_view_show_private_key:
                new DialogPassword(activity, this).show();
                showPrivateText = true;
                break;
            case R.id.tv_private_key_qr_code:
                new DialogPassword(activity, this).show();
                showPrivateText = false;
                break;
            default:
                return;
        }
    }

    @Override
    public void onClick(View v) {
        clickedView = v.getId();
        dismiss();
    }


    @Override
    public void onPasswordEntered(final String password) {
        if (showPrivateText) {
            final DialogProgress dialogProgress = new DialogProgress(this.activity, R.string.please_wait);
            dialogProgress.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    KeyParameter keyParameter = address.getKeyCrypter().deriveKey(password);
                    com.google.bitcoin.core.ECKey decryptedECKey = address.getKeys().get(0);
                    decryptedECKey = decryptedECKey.decrypt(decryptedECKey.getKeyCrypter(), keyParameter);
                    final String str = decryptedECKey.getPrivateKeyEncoded(BitherSetting.NETWORK_PARAMETERS).toString();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            dialogProgress.dismiss();
                            DialogPrivateKeyText dialogPrivateKeyText = new DialogPrivateKeyText(activity, str);
                            dialogPrivateKeyText.show();
                        }
                    });
                }
            }).start();


        } else {
            dialogPrivateKey.show();
        }
    }
}

