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
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.factory.ImportPrivateKey;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.listener.IDialogPasswordListener;

public class DialogImportPrivateKeyText extends CenterDialog implements DialogInterface
        .OnDismissListener, DialogInterface.OnShowListener, View.OnClickListener,
        IDialogPasswordListener {
    public static final int ScanPrivateKeyQRCodeRequestCode = 1603;

    private Activity activity;
    private EditText et;
    private TextView tvError;
    private InputMethodManager imm;

    private String privateKeyString;
    private DialogProgress pd;

    private int clickedId;

    public DialogImportPrivateKeyText(Activity context) {
        super(context);
        this.activity = context;
        setContentView(R.layout.dialog_import_private_key_text);
        et = (EditText) findViewById(R.id.et);
        tvError = (TextView) findViewById(R.id.tv_error);
        et.addTextChangedListener(textWatcher);
        findViewById(R.id.ibtn_scan).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        setOnShowListener(this);
        setOnDismissListener(this);
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void show() {
        clickedId = 0;
        privateKeyString = null;
        et.setText("");
        tvError.setVisibility(View.GONE);
        super.show();
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        if (v.getId() == R.id.btn_ok) {
            String s = et.getText().toString();
            tvError.setText(R.string.import_private_key_text_format_erro);
            if (Utils.isEmpty(s)) {
                tvError.setVisibility(View.VISIBLE);
                shake();
                return;
            } else if (!Utils.validBitcoinPrivateKey(s)) {
                tvError.setVisibility(View.VISIBLE);
                shake();
                return;
            }

            privateKeyString = et.getText().toString();
            dismiss();

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
        if (!Utils.isEmpty(privateKeyString)) {
            DialogPassword d = new DialogPassword(getContext(), this);
            d.show();
        }
        et.setText("");
        if (clickedId == R.id.ibtn_scan) {
            activity.startActivityForResult(new Intent(activity, ScanActivity.class),
                    ScanPrivateKeyQRCodeRequestCode);
        }
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
    public void onPasswordEntered(SecureCharSequence password) {
        pd = new DialogProgress(activity, R.string.please_wait);
        ImportPrivateKey importPrivateKey = new ImportPrivateKey(activity,
                ImportPrivateKey.ImportPrivateKeyType.Text, pd, privateKeyString, password);
        importPrivateKey.importPrivateKey();
    }


}
