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

package net.bither.ui.base.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.SecureCharSequence;

public class IDialogAddressWithShowPrivateKey extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener, IDialogPasswordListener {
    private DialogFancyQrCode dialogQr;
    private DialogPrivateKeyQrCode dialogPrivateKey;
    private Address address;
    private LinearLayout llOriginQRCode;
    private Activity activity;
    private int clickedView;

    public IDialogAddressWithShowPrivateKey(Activity context,
                                            Address address) {
        super(context);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_show_private_key);
        llOriginQRCode = (LinearLayout) findViewById(R.id.ll_origin_qr_code);
        findViewById(R.id.tv_view_show_private_key).setOnClickListener(this);
        findViewById(R.id.tv_private_key_qr_code_decrypted).setOnClickListener(this);
        findViewById(R.id.tv_private_key_qr_code_encrypted).setOnClickListener(this);
        llOriginQRCode.setOnClickListener(this);
        llOriginQRCode.setVisibility(View.GONE);
        findViewById(R.id.tv_close).setOnClickListener(this);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
        dialogPrivateKey = new DialogPrivateKeyQrCode(context, address.getEncryptPrivKey());
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
                break;
            case R.id.tv_private_key_qr_code_encrypted:
                new DialogPassword(activity, this).show();
                break;
            case R.id.tv_private_key_qr_code_decrypted:
                new DialogPassword(activity, this).show();
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
    public void onPasswordEntered(final SecureCharSequence password) {
        final DialogProgress dialogProgress;
        switch (clickedView) {
            case R.id.tv_private_key_qr_code_encrypted:
                dialogPrivateKey.show();
                break;
            case R.id.tv_view_show_private_key:
                dialogProgress = new DialogProgress(this.activity, R.string.please_wait);
                dialogProgress.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ECKey ecKey = PrivateKeyUtil.getECKeyFromSingleString(address.getEncryptPrivKey(), password);
                        final String str = PrivateKeyUtil.getPrivateKeyString(address.getEncryptPrivKey(), password);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                dialogProgress.dismiss();
                                if (str != null) {
                                    DialogPrivateKeyText dialogPrivateKeyText = new DialogPrivateKeyText(activity, str);
                                    dialogPrivateKeyText.show();
                                }
                            }
                        });
                    }

                }).start();

                break;
            case R.id.tv_private_key_qr_code_decrypted:
                dialogProgress = new DialogProgress(this.activity, R.string.please_wait);
                dialogProgress.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String str = PrivateKeyUtil.getPrivateKeyString(address.getEncryptPrivKey(), password);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                dialogProgress.dismiss();
                                if (str != null) {
                                    DialogPrivateKeyTextQrCode dialogPrivateKeyTextQrCode = new DialogPrivateKeyTextQrCode(activity, str);
                                    dialogPrivateKeyTextQrCode.show();
                                }
                            }
                        });

                    }
                }).start();

                break;
        }
    }
}

