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
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.bitcoin.core.ECKey;

import net.bither.R;
import net.bither.activity.hot.HotActivity;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.PrivateKeyUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 14-6-10.
 */
public class DialogImportPrivateKeyText extends CenterDialog implements DialogInterface
        .OnDismissListener, DialogInterface.OnShowListener, View.OnClickListener,
        DialogPassword.DialogPasswordListener {
    private Activity activity;
    private EditText et;
    private TextView tvError;
    private InputMethodManager imm;

    private String privateKeyString;

    public DialogImportPrivateKeyText(Activity context) {
        super(context);
        this.activity = context;
        setContentView(R.layout.dialog_import_private_key_text);
        et = (EditText) findViewById(R.id.et);
        tvError = (TextView) findViewById(R.id.tv_error);
        et.addTextChangedListener(textWatcher);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        setOnShowListener(this);
        setOnDismissListener(this);
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void show() {
        privateKeyString = null;
        et.setText("");
        tvError.setVisibility(View.GONE);
        super.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            String s = et.getText().toString();
            if (StringUtil.isEmpty(s)) {
                tvError.setVisibility(View.VISIBLE);
                shake();
                return;
            }
            privateKeyString = et.getText().toString();
        }
        dismiss();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(et, 0);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!StringUtil.isEmpty(privateKeyString)) {
            DialogPassword d = new DialogPassword(getContext(), this);
            d.show();
        }
        et.setText("");
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            tvError.setVisibility(View.GONE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    @Override
    public void onPasswordEntered(String password) {
        new ImportPrivateKeyThread(privateKeyString, password).start();
    }

    private class ImportPrivateKeyThread extends ThreadNeedService {
        private String privateKey;
        private String password;

        public ImportPrivateKeyThread(String privateKey, String password) {
            super(new DialogProgress(getContext(), R.string.import_private_key_qr_code_importing)
                    , getContext());
            this.privateKey = privateKey;
            this.password = password;
        }

        @Override
        public void runWithService(BlockchainService service) {
            ECKey key = PrivateKeyUtil.getEncryptedECKey(privateKey, password);
            if (key == null) {
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_qr_code_failed);
                    }
                });
                return;
            }
            BitherAddressWithPrivateKey wallet = new BitherAddressWithPrivateKey(false);
            wallet.setKeyCrypter(key.getKeyCrypter());
            wallet.addKey(key);
            if (WalletUtils.getWatchOnlyAddressList().contains(wallet)) {
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_qr_code_failed_monitored);
                    }
                });
                return;
            } else if (WalletUtils.getPrivateAddressList().contains(wallet)) {
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dp != null && dp.isShowing()) {
                            dp.setThread(null);
                            dp.dismiss();
                        }
                        DropdownMessage.showDropdownMessage(activity,
                                R.string.import_private_key_qr_code_failed_duplicate);
                    }
                });
                return;
            } else {
                WalletUtils.addAddressWithPrivateKey(service, wallet);
            }

            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity,
                            R.string.import_private_key_qr_code_success);
                    if (activity instanceof HotActivity) {
                        HotActivity a = (HotActivity) activity;
                        Fragment f = a.getFragmentAtIndex(1);
                        if (f != null && f instanceof Refreshable) {
                            Refreshable r = (Refreshable) f;
                            r.doRefresh();
                        }
                        a.scrollToFragmentAt(1);
                    }
                }
            });
        }
    }
}
