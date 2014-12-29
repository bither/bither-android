/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.Qr;
import net.bither.qrcode.ScanActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.QrCodeImageView;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogFragmentFancyQrCodePager;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogSignMessageOutput;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 14/12/16.
 */
public class SignMessageActivity extends SwipeRightFragmentActivity implements
        IDialogPasswordListener, DialogFragmentFancyQrCodePager.QrCodeThemeChangeListener {
    public static final String AddressKey = "ADDRESS";
    private static final int ScanRequestCode = 1051;

    private Address address;
    private EditText etInput;
    private TextView tvOutput;
    private FrameLayout flOutput;
    private Button btnSign;
    private Button btnQr;
    private ProgressBar pbSign;
    private ImageView ivArrow;
    private View flAddress;
    private TextView tvAddress;
    private QrCodeImageView ivQr;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_sign_message);
        address = WalletUtils.findPrivateKey(getIntent().getExtras().getString(AddressKey));
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        etInput = (EditText) findViewById(R.id.et_input);
        tvOutput = (TextView) findViewById(R.id.tv_output);
        flOutput = (FrameLayout) findViewById(R.id.fl_output);
        btnSign = (Button) findViewById(R.id.btn_sign);
        btnQr = (Button) findViewById(R.id.btn_qr);
        ivArrow = (ImageView) findViewById(R.id.iv_arrow_down);
        pbSign = (ProgressBar) findViewById(R.id.pb_sign);
        flAddress = findViewById(R.id.fl_address);
        ivQr = (QrCodeImageView) findViewById(R.id.iv_qrcode);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        btnSign.setOnClickListener(signClick);
        btnQr.setOnClickListener(scanClick);
        flOutput.setOnClickListener(outputClick);
        etInput.addTextChangedListener(twInput);
        flAddress.setOnClickListener(copyClick);
        ivQr.setOnClickListener(qrClick);
        tvAddress.setText(WalletUtils.formatHash(address.getAddress(), 4, 12));
        Qr.QrCodeTheme theme = AppSharedPreference.getInstance().getFancyQrCodeTheme();
        ivQr.setContent(address.getAddress(), theme.getFgColor(), theme.getBgColor());
    }

    private View.OnClickListener signClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final String input = etInput.getText().toString().trim();
            if (Utils.isEmpty(input)) {
                return;
            }
            new DialogPassword(SignMessageActivity.this, SignMessageActivity.this).show();
        }
    };

    private View.OnClickListener scanClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SignMessageActivity.this, ScanActivity.class);
            startActivityForResult(intent, ScanRequestCode);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ScanRequestCode && resultCode == RESULT_OK) {
            final String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (Utils.isEmpty(input)) {
                return;
            }
            etInput.setText(input);
            btnQr.setEnabled(true);
            btnSign.setVisibility(View.VISIBLE);
            pbSign.setVisibility(View.INVISIBLE);
            ivArrow.setVisibility(View.INVISIBLE);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private View.OnClickListener outputClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String output = tvOutput.getText().toString().trim();
            if (Utils.isEmpty(output)) {
                return;
            }
            new DialogSignMessageOutput(SignMessageActivity.this, output).show();
        }
    };

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        final String input = etInput.getText().toString().trim();
        if (Utils.isEmpty(input)) {
            return;
        }
        etInput.setEnabled(false);
        pbSign.setVisibility(View.VISIBLE);
        btnSign.setVisibility(View.INVISIBLE);
        btnQr.setEnabled(false);
        ivArrow.setVisibility(View.INVISIBLE);
        flOutput.setVisibility(View.GONE);
        imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
        new Thread() {
            @Override
            public void run() {
                String output = null;
                try {
                    output = address.signMessage(input, password);
                } catch (Exception e) {
                    tvOutput.post(new Runnable() {
                        @Override
                        public void run() {
                            DropdownMessage.showDropdownMessage(SignMessageActivity.this,
                                    R.string.password_wrong);
                        }
                    });
                }
                password.wipe();
                final String o = output;
                tvOutput.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!Utils.isEmpty(o)) {
                            tvOutput.setText(o);
                            flOutput.setVisibility(View.VISIBLE);
                            ivArrow.setVisibility(View.VISIBLE);
                        } else {
                            btnSign.setVisibility(View.VISIBLE);
                        }
                        btnQr.setEnabled(true);
                        etInput.setEnabled(true);
                        pbSign.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }.start();
    }

    private TextWatcher twInput = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            flOutput.setVisibility(View.GONE);
            ivArrow.setVisibility(View.INVISIBLE);
            btnSign.setVisibility(View.VISIBLE);
            btnQr.setEnabled(true);
        }
    };

    private View.OnClickListener copyClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (address != null) {
                String text = address.getAddress();
                StringUtil.copyString(text);
                DropdownMessage.showDropdownMessage(SignMessageActivity.this,
                        R.string.copy_address_success);
            }
        }
    };

    private View.OnClickListener qrClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DialogFragmentFancyQrCodePager.newInstance(address.getAddress())
                    .setQrCodeThemeChangeListener(SignMessageActivity.this).show
                    (SignMessageActivity.this.getSupportFragmentManager(),
                            DialogFragmentFancyQrCodePager.FragmentTag);
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    public void qrCodeThemeChangeTo(Qr.QrCodeTheme theme) {
        ivQr.setContent(address.getAddress(), theme.getFgColor(), theme.getBgColor());
    }
}
