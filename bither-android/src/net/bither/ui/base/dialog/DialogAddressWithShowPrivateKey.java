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
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.SignMessageActivity;
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

public class DialogAddressWithShowPrivateKey extends CenterDialog implements View
        .OnClickListener, DialogInterface.OnDismissListener, IDialogPasswordListener {
    private DialogFancyQrCode dialogQr;
    private DialogPrivateKeyQrCode dialogPrivateKey;
    private Address address;
    private LinearLayout llOriginQRCode;
    private LinearLayout llSignMessage;
    private Activity activity;
    private int clickedView;

    public DialogAddressWithShowPrivateKey(Activity context,
                                           Address address) {
        super(context);
        this.activity = context;
        this.address = address;
        setOnDismissListener(this);
        setContentView(R.layout.dialog_address_with_show_private_key);
        llOriginQRCode = (LinearLayout) findViewById(R.id.ll_origin_qr_code);
        llSignMessage = (LinearLayout) findViewById(R.id.ll_sign_message);
        findViewById(R.id.tv_view_show_private_key).setOnClickListener(this);
        findViewById(R.id.tv_private_key_qr_code_decrypted).setOnClickListener(this);
        findViewById(R.id.tv_private_key_qr_code_encrypted).setOnClickListener(this);
        findViewById(R.id.tv_trash_private_key).setOnClickListener(this);
        llOriginQRCode.setOnClickListener(this);
        llOriginQRCode.setVisibility(View.GONE);
        if(AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD){
            llSignMessage.setVisibility(View.VISIBLE);
            llSignMessage.setOnClickListener(this);
        } else {
            llSignMessage.setVisibility(View.GONE);
        }
        findViewById(R.id.tv_close).setOnClickListener(this);
        dialogQr = new DialogFancyQrCode(context, address.getAddress(), false, true);
        dialogPrivateKey = new DialogPrivateKeyQrCode(context, address.getEncryptPrivKey(),
                address.getAddress());
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
            case R.id.tv_trash_private_key:
                if (address.getBalance() > 0) {
                    new DialogConfirmTask(getContext(), getContext().getString(R.string
                            .trash_with_money_warn), null).show();
                } else {
                    new DialogPassword(activity, this).show();
                }
                break;
            case R.id.ll_sign_message:
                Intent intent = new Intent(activity, SignMessageActivity.class);
                intent.putExtra(SignMessageActivity.AddressKey, address.getAddress());
                activity.startActivity(intent);
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
                password.wipe();
                dialogPrivateKey.show();
                break;
            case R.id.tv_view_show_private_key:
                dialogProgress = new DialogProgress(this.activity, R.string.please_wait);
                dialogProgress.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SecureCharSequence str = PrivateKeyUtil.getDecryptPrivateKeyString(address.getEncryptPrivKey(), password);
                        password.wipe();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                dialogProgress.dismiss();
                                if (str != null) {
                                    DialogPrivateKeyText dialogPrivateKeyText = new DialogPrivateKeyText(activity, str);
                                    dialogPrivateKeyText.show();
                                } else {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialogProgress.dismiss();
                                            DropdownMessage.showDropdownMessage(activity, R.string.decrypt_failed);
                                        }
                                    });
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
                        final SecureCharSequence str = PrivateKeyUtil.getDecryptPrivateKeyString(address.getEncryptPrivKey(), password);
                        password.wipe();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                dialogProgress.dismiss();
                                if (str != null) {
                                    DialogPrivateKeyTextQrCode dialogPrivateKeyTextQrCode = new DialogPrivateKeyTextQrCode(activity, str.toString(), address.getAddress());
                                    dialogPrivateKeyTextQrCode.show();
                                } else {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialogProgress.dismiss();
                                            DropdownMessage.showDropdownMessage(activity, R.string.decrypt_failed);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }).start();
                break;
            case R.id.tv_trash_private_key:
                final DialogProgress dp = new DialogProgress(getContext(),
                        R.string.trashing_private_key, null);
                dp.show();
                new Thread() {
                    @Override
                    public void run() {
                        AddressManager.getInstance().trashPrivKey(address);
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                if (activity instanceof AddressDetailActivity) {
                                    activity.finish();
                                }
                                if (AppSharedPreference.getInstance().getAppMode() ==
                                        BitherjSettings.AppMode.HOT) {
                                    Fragment f = BitherApplication.hotActivity.getFragmentAtIndex
                                            (1);
                                    if (f instanceof HotAddressFragment) {
                                        HotAddressFragment hotAddressFragment =
                                                (HotAddressFragment) f;
                                        hotAddressFragment.refresh();
                                    }
                                } else {
                                    Fragment f = BitherApplication.coldActivity
                                            .getFragmentAtIndex(1);
                                    if (f instanceof ColdAddressFragment) {
                                        ColdAddressFragment coldAddressFragment =
                                                (ColdAddressFragment) f;
                                        coldAddressFragment.refresh();
                                    }
                                }
                            }
                        });
                    }
                }.start();
                break;
        }
    }
}

