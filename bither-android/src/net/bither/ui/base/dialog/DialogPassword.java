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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
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
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.delegate.IPasswordGetter;
import net.bither.bitherj.delegate.IPasswordGetterDelegate;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Check;
import net.bither.model.Check.CheckListener;
import net.bither.model.Check.ICheckAction;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.keyboard.password.PasswordEntryKeyboardView;
import net.bither.ui.base.listener.ICheckPasswordListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.CheckUtil;
import net.bither.util.PasswordStrengthUtil;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DialogPassword extends Dialog implements OnDismissListener,
        TextView.OnEditorActionListener {

    private View container;
    private LinearLayout llInput;
    private LinearLayout llChecking;
    private TextView tvTitle;
    private TextView tvError;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnOk;
    private Button btnCancel;
    private TextView tvPasswordLength;
    private TextView tvPasswordStrength;
    private FrameLayout flPasswordStrength;
    private ProgressBar pbPasswordStrength;
    private PasswordEntryKeyboardView kv;
    private PasswordSeed passwordSeed;
    private IDialogPasswordListener listener;
    private ICheckPasswordListener checkPasswordListener;
    private boolean passwordEntered = false;
    private boolean checkPre = true;
    private boolean cancelable = true;
    private boolean needCancelEvent = false;
    private ExecutorService executor;

    public DialogPassword(Context context, IDialogPasswordListener listener) {
        this(context, false, listener);
    }

    public DialogPassword(Context context, boolean isDarkBg, IDialogPasswordListener listener) {
        super(context, R.style.password_dialog);
        setContentView(isDarkBg ? R.layout.dialog_password_dark_background : R.layout.dialog_password);
        this.listener = listener;
        setOnDismissListener(this);
        passwordSeed = getPasswordSeed();
        initView();
    }

    private void initView() {
        container = findViewById(R.id.fl_container);
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        llChecking = (LinearLayout) findViewById(R.id.ll_checking);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvError = (TextView) findViewById(R.id.tv_error);
        etPassword = (EditText) findViewById(R.id.et_password);
        etPasswordConfirm = (EditText) findViewById(R.id.et_password_confirm);
        btnOk = (Button) findViewById(R.id.btn_ok);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        tvPasswordLength = (TextView) findViewById(R.id.tv_password_length);
        tvPasswordStrength = (TextView) findViewById(R.id.tv_password_strength);
        pbPasswordStrength = (ProgressBar) findViewById(R.id.pb_password_strength);
        flPasswordStrength = (FrameLayout) findViewById(R.id.fl_password_strength);
        kv = (PasswordEntryKeyboardView) findViewById(R.id.kv);
        etPassword.addTextChangedListener(passwordWatcher);
        etPasswordConfirm.addTextChangedListener(passwordWatcher);
        etPassword.setOnEditorActionListener(this);
        etPasswordConfirm.setOnEditorActionListener(this);
        configureCheckPre();
        configureEditTextActionId();
        btnOk.setOnClickListener(okClick);
        btnCancel.setOnClickListener(cancelClick);
        btnOk.setEnabled(false);
        passwordCheck.setCheckListener(passwordCheckListener);
        kv.registerEditText(etPassword, etPasswordConfirm);
    }

    private PasswordSeed getPasswordSeed() {
        return PasswordSeed.getPasswordSeed();
    }

    private void configureCheckPre() {
        if (checkPre) {
            if (passwordSeed != null) {
                etPasswordConfirm.setVisibility(View.GONE);
            } else {
                etPasswordConfirm.setVisibility(View.VISIBLE);
            }
        } else {
            etPasswordConfirm.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (listener != null) {
            if (passwordEntered) {
                listener.onPasswordEntered(new SecureCharSequence(etPassword.getText()));
                etPassword.setText("");
                etPasswordConfirm.setText("");
            } else if (needCancelEvent) {
                listener.onPasswordEntered(null);
            }
        }
    }

    private void checkValid() {
        btnOk.setEnabled(false);
        int passwordLength = etPassword.length();
        if (passwordLength >= 6 && passwordLength <= getContext().getResources().getInteger(R
                .integer.password_length_max)) {
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                int passwordConfirmLength = etPasswordConfirm.length();
                if (passwordConfirmLength >= 6 && passwordConfirmLength <= getContext()
                        .getResources().getInteger(R.integer.password_length_max)) {
                    btnOk.setEnabled(true);
                } else {
                    btnOk.setEnabled(false);
                }
            } else {
                btnOk.setEnabled(true);
            }
        }
        if (checkStrength()) {
            if (passwordLength > 0) {
                flPasswordStrength.setVisibility(View.VISIBLE);
                tvPasswordLength.setVisibility(View.INVISIBLE);
                PasswordStrengthUtil.PasswordStrength strength = PasswordStrengthUtil
                        .checkPassword(etPassword.getText());
                if (pbPasswordStrength.getProgress() != strength.getProgress()) {
                    Rect bounds = pbPasswordStrength.getProgressDrawable().getBounds();
                    pbPasswordStrength.setProgressDrawable(strength.getDrawable());
                    pbPasswordStrength.getProgressDrawable().setBounds(bounds);
                    pbPasswordStrength.setProgress(strength.getProgress());
                    tvPasswordStrength.setText(strength.getNameRes());
                }
            } else {
                flPasswordStrength.setVisibility(View.INVISIBLE);
                tvPasswordLength.setVisibility(View.VISIBLE);
            }
        }
    }

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    private void shakeStrength() {
        if (checkStrength()) {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim
                    .password_wrong_warning);
            flPasswordStrength.startAnimation(shake);
        }
    }

    public void setCheckPre(boolean check) {
        checkPre = check;
        configureCheckPre();
    }

    public void setCheckPasswordListener(ICheckPasswordListener checkPasswordListener) {
        this.checkPasswordListener = checkPasswordListener;
    }

    public void show() {
        if (checkPre) {
            if (etPasswordConfirm.getVisibility() != View.VISIBLE) {
                setTitle(R.string.add_address_generate_address_password_label);
            } else {
                setTitle(R.string.add_address_generate_address_password_set_label);
            }
        }
        if (cancelable) {
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }
        super.show();
    }

    public void setTitle(int resource) {
        tvTitle.setText(resource);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            SecureCharSequence password = new SecureCharSequence(etPassword.getText());
            SecureCharSequence passwordConfirm = new SecureCharSequence(etPasswordConfirm.getText());
            if (passwordSeed == null && checkPre) {
                if (!password.equals(passwordConfirm)) {
                    password.wipe();
                    passwordConfirm.wipe();
                    tvError.setText(R.string.add_address_generate_address_password_not_same);
                    tvError.setVisibility(View.VISIBLE);
                    etPasswordConfirm.requestFocus();
                    return;
                } else if (AppSharedPreference.getInstance().getPasswordStrengthCheck()) {
                    PasswordStrengthUtil.PasswordStrength strength = PasswordStrengthUtil
                            .checkPassword(password);
                    password.wipe();
                    passwordConfirm.wipe();
                    if (!strength.passed()) {
                        etPassword.requestFocus();
                        shakeStrength();
                        return;
                    }
                    if (strength.warning()) {
                        new DialogConfirmTask(getContext(), String.format(getContext().getString
                                (R.string.password_strength_warning), strength.getName()), new
                                Runnable() {
                            @Override
                            public void run() {
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        passwordEntered = true;
                                        dismiss();
                                    }
                                });
                            }
                        }).show();
                        return;
                    }
                }
            }
            password.wipe();
            passwordConfirm.wipe();
            if ((passwordSeed != null && checkPre) || checkPasswordListener != null) {
                ArrayList<Check> checks = new ArrayList<Check>();
                checks.add(passwordCheck);
                executor = CheckUtil.runChecks(checks, 1);
            } else {
                passwordEntered = true;
                dismiss();
            }
        }
    };
    private CheckListener passwordCheckListener = new CheckListener() {

        @Override
        public void onCheckBegin(Check check) {
            llChecking.setVisibility(View.VISIBLE);
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
                passwordEntered = true;
                dismiss();
            } else {
                llChecking.setVisibility(View.GONE);
                llInput.setVisibility(View.VISIBLE);
                etPassword.setText("");
                checkValid();
                tvError.setText(R.string.password_wrong);
                tvError.setVisibility(View.VISIBLE);
                shake();
                kv.showKeyboard();
            }
        }
    };
    private TextWatcher passwordWatcher = new TextWatcher() {
        private SecureCharSequence password;
        private SecureCharSequence passwordConfirm;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (password != null) {
                password.wipe();
            }
            if (passwordConfirm != null) {
                passwordConfirm.wipe();
            }
            password = new SecureCharSequence(etPassword.getText());
            passwordConfirm = new SecureCharSequence(etPasswordConfirm.getText());
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            tvError.setVisibility(View.GONE);
            SecureCharSequence p = new SecureCharSequence(etPassword.getText());
            if (p.length() > 0) {
                if (!Utils.validPassword(p)) {
                    etPassword.setText(password);
                }
            }
            p.wipe();
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                SecureCharSequence pc = new SecureCharSequence(etPasswordConfirm.getText());
                if (pc.length() > 0) {
                    if (!Utils.validPassword(pc)) {
                        etPasswordConfirm.setText(passwordConfirm);
                    }
                }
                pc.wipe();
            }
            checkValid();
            password.wipe();
            passwordConfirm.wipe();
        }
    };

    private boolean checkStrength() {
        return passwordSeed == null && checkPre;
    }

    @Override
    public void setCancelable(boolean flag) {
        this.cancelable = flag;
        super.setCancelable(flag);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    private View.OnClickListener cancelClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            passwordEntered = false;
            dismiss();
        }
    };

    private Check passwordCheck = new Check("", new ICheckAction() {
        @Override
        public boolean check() {
            SecureCharSequence password = new SecureCharSequence(etPassword.getText());
            if (checkPasswordListener != null) {
                boolean result = checkPasswordListener.checkPassword(password);
                password.wipe();
                return result;
            } else if (passwordSeed != null) {
                boolean result = passwordSeed.checkPassword(password);
                password.wipe();
                return result;
            } else {
                return true;
            }
        }
    });

    private void configureEditTextActionId() {
        if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
            etPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_NEXT);
        } else {
            etPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
        }
        etPasswordConfirm.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == etPassword) {
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                return false;
            } else if (btnOk.isEnabled()) {
                okClick.onClick(btnOk);
                return true;
            }
        }
        if (v == etPasswordConfirm && btnOk.isEnabled()) {
            okClick.onClick(btnOk);
            return true;
        }
        return false;
    }

    public void setNeedCancelEvent(boolean needCancelEvent) {
        this.needCancelEvent = needCancelEvent;
    }

    //This class should not be used on main thread
    public static final class PasswordGetter implements IDialogPasswordListener, IPasswordGetter {


        private ReentrantLock getPasswordLock = new ReentrantLock();
        private Condition withPasswordCondition = getPasswordLock.newCondition();
        private Context context;
        private SecureCharSequence password;
        private IPasswordGetterDelegate delegate;

        public PasswordGetter(Context context) {
            this(context, null);
        }

        public PasswordGetter(Context context, IPasswordGetterDelegate delegate) {
            this.context = context;
            this.delegate = delegate;
        }

        public void setPassword(SecureCharSequence password) {
            this.password = password;
        }

        public boolean hasPassword() {
            return password != null;
        }

        public SecureCharSequence getPassword() {
            if (password == null) {
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (delegate != null) {
                            delegate.beforePasswordDialogShow();
                        }
                        DialogPassword d = new DialogPassword(context, PasswordGetter.this);
                        d.setNeedCancelEvent(true);
                        d.show();
                    }
                });
                try {
                    getPasswordLock.lockInterruptibly();
                    withPasswordCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    getPasswordLock.unlock();
                }
            }
            return password;
        }

        @Override
        public void onPasswordEntered(SecureCharSequence password) {
            setPassword(password);
            try {
                getPasswordLock.lock();
                withPasswordCondition.signal();
            } finally {
                getPasswordLock.unlock();
            }
            if (delegate != null && password != null) {
                delegate.afterPasswordDialogDismiss();
            }
        }

        public void wipe() {
            if (password != null) {
                password.wipe();
                password = null;
            }
        }
    }
}
