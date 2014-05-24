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

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
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
import net.bither.model.Check.CheckListener;
import net.bither.model.Check.ICheckAction;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.util.CheckUtil;
import net.bither.util.StringUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class DialogPassword extends CenterDialog implements OnDismissListener,
        OnShowListener {
    private LinearLayout llInput;
    private LinearLayout llChecking;
    private TextView tvTitle;
    private TextView tvError;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnOk;
    private Button btnCancel;
    private PasswordSeed passwordSeed;
    private Check passwordCheck = new Check("", new ICheckAction() {
        @Override
        public boolean check() {
            if (passwordSeed != null) {
                return passwordSeed.checkPassword(etPassword.getText()
                        .toString());
            } else {
                return true;
            }
        }
    });
    private DialogPasswordListener listener;
    private boolean passwordEntered = false;
    private View.OnClickListener cancelClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            passwordEntered = false;
            dismiss();
        }
    };
    private boolean checkPre = true;
    private boolean cancelable = true;
    private InputMethodManager imm;
    private ExecutorService executor;
    private View.OnClickListener okClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (passwordSeed == null
                    && !StringUtil
                    .compareString(etPassword.getText().toString(),
                            etPasswordConfirm.getText().toString())
                    && checkPre) {
                tvError.setText(R.string.add_address_generate_address_password_not_same);
                tvError.setVisibility(View.VISIBLE);
                etPasswordConfirm.requestFocus();
                return;
            }
            if (passwordSeed != null && checkPre) {
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
                imm.showSoftInput(etPassword, 0);
            }
        }

        @Override
        public void onCheckBegin(Check check) {
            llChecking.setVisibility(View.VISIBLE);
            llInput.setVisibility(View.INVISIBLE);
            imm.hideSoftInputFromWindow(etPassword.getWindowToken(), 0);
        }
    };
    private TextWatcher passwordWatcher = new TextWatcher() {
        private String password;
        private String passwordConfirm;

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            password = etPassword.getText().toString();
            passwordConfirm = etPasswordConfirm.getText().toString();
        }

        @Override
        public void afterTextChanged(Editable s) {
            tvError.setVisibility(View.GONE);
            String p = etPassword.getText().toString();
            if (p.length() > 0) {
                if (!StringUtil.validPassword(p)) {
                    etPassword.setText(password);
                }
            }
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                String pc = etPasswordConfirm.getText().toString();
                if (pc.length() > 0) {
                    if (!StringUtil.validPassword(pc)) {
                        etPasswordConfirm.setText(passwordConfirm);
                    }
                }
            }
            checkValid();
        }
    };

    public DialogPassword(Context context, DialogPasswordListener listener) {
        super(context);
        setContentView(R.layout.dialog_password);
        this.listener = listener;
        setOnDismissListener(this);
        setOnShowListener(this);
        passwordSeed = getPasswordSeed();
        initView();
    }

    ;

    private void initView() {
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        llChecking = (LinearLayout) findViewById(R.id.ll_checking);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvError = (TextView) findViewById(R.id.tv_error);
        etPassword = (EditText) findViewById(R.id.et_password);
        etPasswordConfirm = (EditText) findViewById(R.id.et_password_confirm);
        btnOk = (Button) findViewById(R.id.btn_ok);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        etPassword.addTextChangedListener(passwordWatcher);
        etPasswordConfirm.addTextChangedListener(passwordWatcher);
        configureCheckPre();
        btnOk.setOnClickListener(okClick);
        btnCancel.setOnClickListener(cancelClick);
        btnOk.setEnabled(false);
        passwordCheck.setCheckListener(passwordCheckListener);
        imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
    }

    public void dismiss() {
        imm.hideSoftInputFromWindow(etPassword.getWindowToken(), 0);
        super.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (passwordEntered && listener != null) {
            listener.onPasswordEntered(etPassword.getText().toString());
        }
    }

    private void checkValid() {
        btnOk.setEnabled(false);
        String password = etPassword.getText().toString();
        if (password.length() >= 6 && password.length() <= 20) {
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                String passwordConfirm = etPasswordConfirm.getText().toString();
                if (passwordConfirm.length() >= 6
                        && passwordConfirm.length() <= 20) {
                    btnOk.setEnabled(true);
                } else {
                    btnOk.setEnabled(false);
                }
            } else {
                btnOk.setEnabled(true);
            }
        }
    }

    private PasswordSeed getPasswordSeed() {
        return AppSharedPreference.getInstance().getPasswordSeed();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(etPassword, 0);
    }

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(),
                R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    public void setCheckPre(boolean check) {
        checkPre = check;
        configureCheckPre();
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

    ;

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    @Override
    public void setCancelable(boolean flag) {
        this.cancelable = flag;
        super.setCancelable(flag);
    }

    public static interface DialogPasswordListener {
        public void onPasswordEntered(String password);
    }
}
