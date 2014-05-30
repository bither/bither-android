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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.model.Check;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.EditPasswordThread;
import net.bither.util.CheckUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Created by songchenwen on 14-5-24.
 */
public class DialogEditPassword extends CenterDialog implements DialogInterface.OnShowListener,
        Check.CheckListener, Check.ICheckAction, View.OnClickListener,
        EditPasswordThread.EditPasswordListener {
    private Activity activity;
    private LinearLayout llInput;
    private LinearLayout llEditing;
    private TextView tvError;
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etNewPasswordConfirm;
    private Button btnOk;
    private PasswordSeed passwordSeed;
    private Check passwordCheck = new Check("", this);
    private InputMethodManager imm;
    private ExecutorService executor;

    public DialogEditPassword(Activity context) {
        super(context);
        activity = context;
        setContentView(R.layout.dialog_edit_password);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        setOnShowListener(this);
        passwordSeed = getPasswordSeed();
        initView();
    }

    private PasswordSeed getPasswordSeed() {
        return AppSharedPreference.getInstance().getPasswordSeed();
    }

    private void initView() {
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        llEditing = (LinearLayout) findViewById(R.id.ll_editing);
        tvError = (TextView) findViewById(R.id.tv_error);
        etOldPassword = (EditText) findViewById(R.id.et_old_password);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);
        etNewPasswordConfirm = (EditText) findViewById(R.id.et_new_password_confirm);
        btnOk = (Button) findViewById(R.id.btn_ok);
        PasswordWatcher watcher = new PasswordWatcher();
        etOldPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etNewPasswordConfirm.addTextChangedListener(watcher);
        btnOk.setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnOk.setEnabled(false);
        passwordCheck.setCheckListener(this);
        imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        UIUtil.configurePasswordEditText(etOldPassword);
        UIUtil.configurePasswordEditText(etNewPassword);
        UIUtil.configurePasswordEditText(etNewPasswordConfirm);
    }

    @Override
    public void show() {
        if (passwordSeed != null) {
            super.show();
        }
    }

    public void dismiss() {
        imm.hideSoftInputFromWindow(etNewPassword.getWindowToken(), 0);
        super.dismiss();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(etOldPassword, 0);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            if (!StringUtil
                    .compareString(etNewPassword.getText().toString(),
                            etNewPasswordConfirm.getText().toString())) {
                shake();
                tvError.setText(R.string.add_address_generate_address_password_not_same);
                tvError.setVisibility(View.VISIBLE);
                etNewPasswordConfirm.requestFocus();
                return;
            }
            if (StringUtil.compareString(etOldPassword.getText().toString(),
                    etNewPassword.getText().toString())) {
                shake();
                tvError.setText(R.string.edit_password_new_old_same);
                tvError.setVisibility(View.VISIBLE);
                etNewPasswordConfirm.requestFocus();
                return;
            }
            if (passwordSeed != null) {
                ArrayList<Check> checks = new ArrayList<Check>();
                checks.add(passwordCheck);
                executor = CheckUtil.runChecks(checks, 1);
            } else {
                dismiss();
            }
        } else {
            dismiss();
        }
    }

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(),
                R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    @Override
    public boolean check() {
        if (passwordSeed != null) {
            return passwordSeed.checkPassword(etOldPassword.getText()
                    .toString());
        } else {
            return true;
        }
    }

    @Override
    public void onCheckBegin(Check check) {
        llEditing.setVisibility(View.VISIBLE);
        llInput.setVisibility(View.INVISIBLE);
        imm.hideSoftInputFromWindow(etOldPassword.getWindowToken(), 0);
    }

    @Override
    public void onCheckEnd(Check check, boolean success) {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        if (success) {
            editPassword();
        } else {
            llEditing.setVisibility(View.GONE);
            llInput.setVisibility(View.VISIBLE);
            etOldPassword.setText("");
            checkValid();
            tvError.setText(R.string.password_wrong);
            tvError.setVisibility(View.VISIBLE);
            shake();
            imm.showSoftInput(etOldPassword, 0);
        }
    }

    private void editPassword() {
        new EditPasswordThread(etOldPassword.getText().toString(),
                etNewPassword.getText().toString(), this).start();
    }

    private void checkValid() {
        btnOk.setEnabled(false);
        String passwordOld = etOldPassword.getText().toString();
        if (passwordOld.length() >= 6 && passwordOld.length() <= 30) {
            String password = etNewPassword.getText().toString();
            if (password.length() >= 6 && password.length() <= 30) {
                String passwordConfirm = etNewPasswordConfirm.getText().toString();
                if (passwordConfirm.length() >= 6
                        && passwordConfirm.length() <= 30) {
                    btnOk.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void onSuccess() {
        dismiss();
        DropdownMessage.showDropdownMessage(activity, R.string.edit_password_success);
    }

    @Override
    public void onFailed() {
        dismiss();
        DropdownMessage.showDropdownMessage(activity, R.string.edit_password_fail);
    }

    private class PasswordWatcher implements TextWatcher {
        private String passwordOld;
        private String passwordNew;
        private String passwordNewConfirm;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            passwordOld = etOldPassword.getText().toString();
            passwordNew = etNewPassword.getText().toString();
            passwordNewConfirm = etNewPasswordConfirm.getText().toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            tvError.setVisibility(View.GONE);
            String p = etNewPassword.getText().toString();
            if (p.length() > 0) {
                if (!StringUtil.validPassword(p)) {
                    etNewPassword.setText(passwordNew);
                }
            }
            String pc = etNewPasswordConfirm.getText().toString();
            if (pc.length() > 0) {
                if (!StringUtil.validPassword(pc)) {
                    etNewPasswordConfirm.setText(passwordNewConfirm);
                }
            }
            String po = etOldPassword.getText().toString();
            if (po.length() > 0) {
                if (!StringUtil.validPassword(po)) {
                    etOldPassword.setText(passwordOld);
                }
            }
            checkValid();
        }
    }
}
