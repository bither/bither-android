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
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.bip38.Bip38;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.factory.ImportPrivateKey;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.listener.DialogPasswordListener;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.util.SecureCharSequence;
import net.bither.util.StringUtil;

public class DialogImportBip38KeyText extends CenterDialog implements DialogInterface
        .OnDismissListener, DialogInterface.OnShowListener, View.OnClickListener,
        DialogPasswordListener {
    private Activity activity;
    private EditText et;
    private TextView tvError;
    private InputMethodManager imm;

    private String bip38KeyString;

    private String decode;
    private DialogProgress pd;

    public DialogImportBip38KeyText(Activity context) {
        super(context);
        this.activity = context;
        setContentView(R.layout.dialog_import_bip38_key_text);
        et = (EditText) findViewById(R.id.et);
        tvError = (TextView) findViewById(R.id.tv_error);
        et.addTextChangedListener(textWatcher);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        setOnShowListener(this);
        setOnDismissListener(this);
        pd = new DialogProgress(activity, R.string.please_wait);
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void show() {
        bip38KeyString = null;
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
            try {
                if (!Bip38.isBip38PrivateKey(s)) {
                    tvError.setVisibility(View.VISIBLE);
                    shake();
                    return;
                }
                bip38KeyString = et.getText().toString();
                dismiss();

            } catch (AddressFormatException e) {
                tvError.setVisibility(View.VISIBLE);
                e.printStackTrace();
            }
        } else {
            dismiss();
        }
    }


    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(et, 0);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!StringUtil.isEmpty(bip38KeyString)) {
            showBip38Password();
        }
        et.setText("");

    }

    private void showBip38Password() {
        DialogPasswordWithOther d = new DialogPasswordWithOther(getContext(), bip38DialogPasswordListener);
        d.setCheckPre(false);
        d.setTitle(R.string.enter_bip38_key_password);
        d.setCheckPasswordListener(new ICheckPasswordListener() {
            @Override
            public boolean checkPassword(SecureCharSequence password) {
                try {
                    decode = Bip38.decrypt(bip38KeyString, password.toString());
                    return decode != null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
        d.show();
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
    public void onPasswordEntered(final SecureCharSequence password) {
        ImportPrivateKey importPrivateKey = new ImportPrivateKey(activity,
                ImportPrivateKey.ImportPrivateKeyType.Bip38, pd, decode, password);
        importPrivateKey.importPrivateKey();
    }

    private DialogPasswordListener bip38DialogPasswordListener = new DialogPasswordListener() {
        @Override
        public void onPasswordEntered(final SecureCharSequence password) {
            if (decode != null) {
                DialogPassword dialogPassword = new DialogPassword(getContext(), DialogImportBip38KeyText.this);
                dialogPassword.show();
            } else {
                DropdownMessage.showDropdownMessage(activity, R.string.password_wrong);
                showBip38Password();
            }
        }
    };

}
