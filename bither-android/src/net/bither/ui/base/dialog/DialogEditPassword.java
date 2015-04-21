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
import android.app.Dialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.runnable.EditPasswordThread;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Check;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.keyboard.password.PasswordEntryKeyboardView;
import net.bither.util.BackupUtil;
import net.bither.util.CheckUtil;
import net.bither.util.PasswordStrengthUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class DialogEditPassword extends Dialog implements Check.CheckListener,
        Check.ICheckAction, View.OnClickListener, EditPasswordThread.EditPasswordListener,
        TextView.OnEditorActionListener {
    private Activity activity;
    private View container;
    private LinearLayout llInput;
    private LinearLayout llEditing;
    private TextView tvError;
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etNewPasswordConfirm;
    private TextView tvPasswordStrength;
    private FrameLayout flPasswordStrength;
    private FrameLayout flPasswordStrengthContainer;
    private ProgressBar pbPasswordStrength;
    private Button btnOk;
    private PasswordEntryKeyboardView kv;
    private PasswordSeed passwordSeed;
    private Check passwordCheck = new Check("", this);
    private ExecutorService executor;

    public DialogEditPassword(Activity context) {
        super(context, R.style.password_dialog);
        activity = context;
        setContentView(R.layout.dialog_edit_password);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        passwordSeed = getPasswordSeed();
        initView();
    }

    private PasswordSeed getPasswordSeed() {
        return PasswordSeed.getPasswordSeed();
    }

    private void initView() {
        container = findViewById(R.id.fl_container);
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        llEditing = (LinearLayout) findViewById(R.id.ll_editing);
        tvError = (TextView) findViewById(R.id.tv_error);
        etOldPassword = (EditText) findViewById(R.id.et_old_password);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);
        etNewPasswordConfirm = (EditText) findViewById(R.id.et_new_password_confirm);
        btnOk = (Button) findViewById(R.id.btn_ok);
        tvPasswordStrength = (TextView) findViewById(R.id.tv_password_strength);
        pbPasswordStrength = (ProgressBar) findViewById(R.id.pb_password_strength);
        flPasswordStrength = (FrameLayout) findViewById(R.id.fl_password_strength);
        flPasswordStrengthContainer = (FrameLayout) findViewById(R.id
                .fl_password_strength_container);
        kv = (PasswordEntryKeyboardView) findViewById(R.id.kv);
        PasswordWatcher watcher = new PasswordWatcher();
        etOldPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etNewPasswordConfirm.addTextChangedListener(watcher);
        btnOk.setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnOk.setEnabled(false);
        passwordCheck.setCheckListener(this);
        etOldPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_NEXT);
        etNewPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_NEXT);
        etNewPasswordConfirm.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
        etNewPasswordConfirm.setOnEditorActionListener(this);
        kv.registerEditText(etOldPassword, etNewPassword, etNewPasswordConfirm);
    }

    @Override
    public void show() {
        if (passwordSeed != null) {
            super.show();
        } else if (activity != null) {
            DropdownMessage.showDropdownMessage(activity, R.string.private_key_is_empty);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            SecureCharSequence oldP = new SecureCharSequence(etOldPassword.getText());
            SecureCharSequence newP = new SecureCharSequence(etNewPassword.getText());
            SecureCharSequence newCP = new SecureCharSequence(etNewPasswordConfirm.getText());
            if (!newP.equals(newCP)) {
                shake();
                tvError.setText(R.string.add_address_generate_address_password_not_same);
                tvError.setVisibility(View.VISIBLE);
                etNewPasswordConfirm.requestFocus();
                oldP.wipe();
                newP.wipe();
                newCP.wipe();
                return;
            }
            if (oldP.equals(newP)) {
                shake();
                tvError.setText(R.string.edit_password_new_old_same);
                tvError.setVisibility(View.VISIBLE);
                etNewPasswordConfirm.requestFocus();
                oldP.wipe();
                newP.wipe();
                newCP.wipe();
                return;
            }
            if (AppSharedPreference.getInstance().getPasswordStrengthCheck()) {
                PasswordStrengthUtil.PasswordStrength strength = PasswordStrengthUtil
                        .checkPassword(newP);
                oldP.wipe();
                newP.wipe();
                newCP.wipe();
                if (!strength.passed()) {
                    etNewPassword.requestFocus();
                    shakeStrength();
                    return;
                }
                if (strength.warning()) {
                    new DialogConfirmTask(getContext(), String.format(getContext().getString(R
                            .string.password_strength_warning), strength.getName()), new Runnable
                            () {
                        @Override
                        public void run() {
                            executor = CheckUtil.runChecks(Arrays.asList(new
                                    Check[]{passwordCheck}), 1);
                        }
                    }).show();
                    return;
                }
            }
            oldP.wipe();
            newP.wipe();
            newCP.wipe();
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
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    private void shakeStrength() {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        flPasswordStrength.startAnimation(shake);
    }

    @Override
    public boolean check() {
        if (passwordSeed != null) {
            SecureCharSequence password = new SecureCharSequence(etOldPassword.getText());
            boolean result = passwordSeed.checkPassword(password);
            password.wipe();
            return result;
        } else {
            return true;
        }
    }

    @Override
    public void onCheckBegin(Check check) {
        llEditing.setVisibility(View.VISIBLE);
        llInput.setVisibility(View.INVISIBLE);
        kv.hideKeyboard();
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
            kv.showKeyboard();
        }
    }

    private void editPassword() {
        new EditPasswordThread(new SecureCharSequence(etOldPassword.getText()),
                new SecureCharSequence(etNewPassword.getText()), this).start();
    }

    private void checkValid() {
        btnOk.setEnabled(false);
        int passwordOldLength = etOldPassword.length();
        int passwordLength = etNewPassword.length();
        if (passwordOldLength >= 6 && passwordOldLength <= getContext().getResources().getInteger
                (R.integer.password_length_max)) {
            if (passwordLength >= 6 && passwordLength <= getContext().getResources().getInteger(R
                    .integer.password_length_max)) {
                int passwordConfirmLength = etNewPasswordConfirm.length();
                if (passwordConfirmLength >= 6 && passwordConfirmLength <= getContext()
                        .getResources().getInteger(R.integer.password_length_max)) {
                    btnOk.setEnabled(true);
                }
            }
        }
        if (passwordLength > 0) {
            ViewGroup.LayoutParams lp = flPasswordStrengthContainer.getLayoutParams();
            if (lp.height < etNewPassword.getHeight() + flPasswordStrength.getHeight() + UIUtil
                    .dip2pix(5)) {
                lp.height = etNewPassword.getHeight() + flPasswordStrength.getHeight() + UIUtil
                        .dip2pix(5);
                flPasswordStrengthContainer.requestLayout();
            }
            flPasswordStrength.setVisibility(View.VISIBLE);
            PasswordStrengthUtil.PasswordStrength strength = PasswordStrengthUtil.checkPassword
                    (etNewPassword.getText());
            pbPasswordStrength.setProgress(strength.getProgress());
            pbPasswordStrength.setProgressDrawable(strength.getDrawable());
            tvPasswordStrength.setText(strength.getNameRes());
        } else {
            ViewGroup.LayoutParams lp = flPasswordStrengthContainer.getLayoutParams();
            if (lp.height > etNewPassword.getHeight() + UIUtil.dip2pix(5)) {
                lp.height = etNewPassword.getHeight() + UIUtil.dip2pix(5);
                flPasswordStrengthContainer.requestLayout();
            }
            flPasswordStrength.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSuccess() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                dismiss();
                DropdownMessage.showDropdownMessage(activity, R.string.edit_password_success);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (AbstractApp.bitherjSetting.getAppMode() == BitherjSettings.AppMode.COLD) {
                            BackupUtil.backupColdKey(false);
                        } else {
                            BackupUtil.backupHotKey();
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    public void onFailed() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                dismiss();
                DropdownMessage.showDropdownMessage(activity, R.string.edit_password_fail);
            }
        });

    }

    @Override
    public void dismiss() {
        super.dismiss();
        etOldPassword.setText("");
        etNewPassword.setText("");
        etNewPasswordConfirm.setText("");
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == etNewPasswordConfirm && btnOk.isEnabled()) {
            onClick(btnOk);
            return true;
        }
        return false;
    }

    private class PasswordWatcher implements TextWatcher {
        private SecureCharSequence passwordOld;
        private SecureCharSequence passwordNew;
        private SecureCharSequence passwordNewConfirm;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (passwordOld != null) {
                passwordOld.wipe();
            }
            if (passwordNew != null) {
                passwordNew.wipe();
            }
            if (passwordNewConfirm != null) {
                passwordNewConfirm.wipe();
            }
            passwordOld = new SecureCharSequence(etOldPassword.getText());
            passwordNew = new SecureCharSequence(etNewPassword.getText());
            passwordNewConfirm = new SecureCharSequence(etNewPasswordConfirm.getText());
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            tvError.setVisibility(View.GONE);
            SecureCharSequence p = new SecureCharSequence(etNewPassword.getText());
            if (p.length() > 0) {
                if (!Utils.validPassword(p)) {
                    etNewPassword.setText(passwordNew);
                }
            }
            p.wipe();
            SecureCharSequence pc = new SecureCharSequence(etNewPasswordConfirm.getText());
            if (pc.length() > 0) {
                if (!Utils.validPassword(pc)) {
                    etNewPasswordConfirm.setText(passwordNewConfirm);
                }
            }
            pc.wipe();
            SecureCharSequence po = new SecureCharSequence(etOldPassword.getText());
            if (po.length() > 0) {
                if (!Utils.validPassword(po)) {
                    etOldPassword.setText(passwordOld);
                }
            }
            po.wipe();
            checkValid();
            passwordOld.wipe();
            passwordNew.wipe();
            passwordNewConfirm.wipe();
        }
    }
}
