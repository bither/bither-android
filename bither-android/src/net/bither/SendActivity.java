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

package net.bither;

import static net.bither.BitherSetting.INTENT_REF.MINER_FEE_BASE_KEY;
import static net.bither.BitherSetting.INTENT_REF.MINER_FEE_MODE_KEY;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.activity.hot.MinerFeeSettingActivity;
import net.bither.activity.hot.SelectAddressToSendActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.api.BitherStatsDynamicFeeApi;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Ticker;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.CommitTransactionThread;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.runnable.RCheckRunnable;
import net.bither.ui.base.CurrencyAmountView;
import net.bither.ui.base.CurrencyCalculatorLink;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogRCheck;
import net.bither.ui.base.dialog.DialogSelectChangeAddress;
import net.bither.ui.base.dialog.DialogSendConfirm;
import net.bither.ui.base.dialog.DialogSendConfirm.SendConfirmListener;
import net.bither.ui.base.dialog.DialogSendOption;
import net.bither.ui.base.keyboard.EntryKeyboardView;
import net.bither.ui.base.keyboard.amount.AmountEntryKeyboardView;
import net.bither.ui.base.keyboard.password.PasswordEntryKeyboardView;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.BroadcastUtil;
import net.bither.util.InputParser.StringInputParser;
import net.bither.util.MarketUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UnitUtilWrapper;

import java.math.BigInteger;

public class SendActivity extends SwipeRightActivity implements EntryKeyboardView
        .EntryKeyboardViewListener, CommitTransactionThread.CommitTransactionListener,
        DialogSendOption.DialogSendOptionListener {
    protected int addressPosition;
    protected Address address;
    private TextView tvAddressLabel;

    protected EditText etAddress;
    protected EditText etPassword;
    private ImageButton ibtnScan;
    protected CurrencyCalculatorLink amountCalculatorLink;
    protected Button btnSend;
    protected DialogRCheck dp;
    private TextView tvBalance;
    private ImageView ivBalanceSymbol;
    private PasswordEntryKeyboardView kvPassword;
    private AmountEntryKeyboardView kvAmount;
    private View vKeyboardContainer;
    private LinearLayout llMinerFee;
    private TextView tvMinerFee;
    private ImageView ivMinerFeeDes;
    protected DialogSelectChangeAddress dialogSelectChangeAddress;
    protected boolean isColdSendBtc;

    private boolean isDonate = false;
    private MinerFeeSettingActivity.MinerFeeMode minerFeeMode;
    private long minerFeeBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_send);
        initAddress();
        if (address == null) {
            finish();
            return;
        }
        initView();
        processIntent();
        configureDonate();
        if (AppSharedPreference.getInstance().isUseDynamicMinerFee()) {
            minerFeeMode = MinerFeeSettingActivity.MinerFeeMode.Dynamic;
        } else {
            MinerFeeSettingActivity.MinerFeeMode mode = MinerFeeSettingActivity.MinerFeeMode.getMinerFeeMode(AppSharedPreference.getInstance().getTransactionFeeMode());
            if (mode != null) {
                minerFeeMode = mode;
                minerFeeBase = mode.getFeeBase();
            } else {
                minerFeeMode = MinerFeeSettingActivity.MinerFeeMode.Dynamic;
            }
        }
        showMinerFee();
    }

    protected void initAddress() {
        if (getIntent().getExtras().containsKey(BitherSetting.INTENT_REF
                .ADDRESS_POSITION_PASS_VALUE_TAG)) {
            addressPosition = getIntent().getExtras().getInt(BitherSetting.INTENT_REF
                    .ADDRESS_POSITION_PASS_VALUE_TAG);
            if (addressPosition >= 0 && addressPosition < AddressManager.getInstance()
                    .getPrivKeyAddresses().size()) {
                address = AddressManager.getInstance().getPrivKeyAddresses().get(addressPosition);
            }
        }
    }

    private void initView() {
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener());
        tvAddressLabel = (TextView) findViewById(R.id.tv_address_label);
        etAddress = (EditText) findViewById(R.id.et_address);
        ibtnScan = (ImageButton) findViewById(R.id.ibtn_scan);
        btnSend = (Button) findViewById(R.id.btn_send);
        etPassword = (EditText) findViewById(R.id.et_password);
        tvBalance = (TextView) findViewById(R.id.tv_balance);
        ivBalanceSymbol = (ImageView) findViewById(R.id.iv_balance_symbol);
        kvPassword = (PasswordEntryKeyboardView) findViewById(R.id.kv_password);
        kvAmount = (AmountEntryKeyboardView) findViewById(R.id.kv_amount);
        vKeyboardContainer = findViewById(R.id.v_keyboard_container);
        llMinerFee = findViewById(R.id.ll_miner_fee);
        llMinerFee.setOnClickListener(llMinerFeeListener);
        tvMinerFee = findViewById(R.id.tv_miner_fee);
        ivMinerFeeDes = findViewById(R.id.iv_miner_fee_des);
        ivMinerFeeDes.setOnClickListener(ivMinerFeeDesClick);
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        dialogSelectChangeAddress = new DialogSelectChangeAddress(this, address);
        tvBalance.setText(UnitUtilWrapper.formatValue(address.getBalance()));
        ivBalanceSymbol.setImageBitmap(UnitUtilWrapper.getBtcSymbol(tvBalance));
        etPassword.addTextChangedListener(passwordWatcher);
        etPassword.setOnEditorActionListener(passwordAction);
        final CurrencyAmountView btcAmountView = (CurrencyAmountView) findViewById(R.id.cav_btc);
        btcAmountView.setCurrencySymbol(getString(R.string.bitcoin_symbol));
        int precision = (int) Math.floor(Math.log10(AppSharedPreference.getInstance()
                .getBitcoinUnit().satoshis));
        btcAmountView.setInputPrecision(precision);
        btcAmountView.setHintPrecision(Math.min(4, precision));
        btcAmountView.setShift(8 - precision);

        final CurrencyAmountView localAmountView = (CurrencyAmountView) findViewById(R.id
                .cav_local);
        localAmountView.setInputPrecision(2);
        localAmountView.setHintPrecision(2);
        amountCalculatorLink = new CurrencyCalculatorLink(btcAmountView, localAmountView);
        ReceivingAddressListener addressListener = new ReceivingAddressListener();
        etAddress.setOnFocusChangeListener(addressListener);
        etAddress.addTextChangedListener(addressListener);
        dp = new DialogRCheck(this);
        ibtnScan.setOnClickListener(scanClick);
        btnSend.setOnClickListener(sendClick);
        kvPassword.registerEditText(etPassword).setListener(this);
        kvAmount.registerEditText((EditText) findViewById(R.id.send_coins_amount_btc_edittext),
                (EditText) findViewById(R.id.send_coins_amount_local_edittext)).setListener(this);
        findViewById(R.id.ll_balance).setOnClickListener(balanceClick);
    }

    private OnClickListener balanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            amountCalculatorLink.setBtcAmount(address.getBalance());
        }
    };


    private OnClickListener scanClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SendActivity.this, ScanActivity.class);
            startActivityForResult(intent, BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
        }
    };

    private SendConfirmListener sendConfirmListener = new SendConfirmListener() {

        @Override
        public void onConfirm(Tx tx) {
            etPassword.setText("");
            try {
                dp.setWait();
                new CommitTransactionThread(dp, addressPosition, tx, false, true, SendActivity.this).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCancel() {

        }
    };

    private Handler completeTransactionHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_SUCCESS:
                    if (!dp.isShowing()) {
                        dp.show();
                    }
                    if (msg.obj != null && msg.obj instanceof Tx) {
                        Tx tx = (Tx) msg.obj;
                        //dp.setRChecking();
                        RCheckRunnable run = new RCheckRunnable(address, tx);
                        run.setHandler(rcheckHandler);
                        new Thread(run).start();
                    } else {
                        DropdownMessage.showDropdownMessage(SendActivity.this,
                                R.string.password_wrong);
                    }
                    break;
                case HandlerMessage.MSG_PASSWORD_WRONG:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(SendActivity.this, R.string.password_wrong);
                    break;
                case HandlerMessage.MSG_FAILURE:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    String msgError = getString(R.string.send_failed);
                    if (msg.obj instanceof String) {
                        msgError = (String) msg.obj;
                    }
                    DropdownMessage.showDropdownMessage(SendActivity.this, msgError);
                    break;
                default:
                    break;
            }
        }
    };

    private Handler rcheckHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_SUCCESS:
                    if (msg.obj != null && msg.obj instanceof Tx) {
                        final Tx tx = (Tx) msg.obj;
                        // dp.setRCheckSuccess();
                        if (!dp.isShowing()) {
                            dp.show();
                        }
                        tvAddressLabel.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                DialogSendConfirm dialog = new DialogSendConfirm(SendActivity
                                        .this, tx, dialogSelectChangeAddress.getChangeAddress()
                                        .equals(address) ? null : dialogSelectChangeAddress
                                        .getChangeAddress().getAddress(), sendConfirmListener);
                                dialog.show();
                                dp.setWait();
                            }
                        }, 800);
                        break;
                    }
                case HandlerMessage.MSG_FAILURE:
                    //TODO need more complicated logic to recalculate r,
                    // because rfc6979 will use the same r for the same transaction
                    // dp.setRecalculatingR();
                    if (!dp.isShowing()) {
                        dp.show();
                    }
                    sendClick.onClick(btnSend);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCommitTransactionSuccess(Tx tx) {
        Intent intent = getIntent();
        if (tx != null) {
            intent.putExtra(SelectAddressToSendActivity.INTENT_EXTRA_TRANSACTION,
                    tx.getHashAsString());
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCommitTransactionFailed() {
        DropdownMessage.showDropdownMessage(SendActivity.this, R.string.send_failed);
    }

    private OnClickListener ivMinerFeeDesClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogConfirmTask confirmTask = new DialogConfirmTask(SendActivity.this, getString(R.string.dynamic_miner_fee_des), new Runnable() {
                @Override
                public void run() {  }
            }, false);
            confirmTask.setCancelable(false);
            confirmTask.show();
        }
    };

    private View.OnClickListener llMinerFeeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(SendActivity.this, MinerFeeSettingActivity.class);
            intent.putExtra(MINER_FEE_MODE_KEY, minerFeeMode);
            intent.putExtra(MINER_FEE_BASE_KEY, minerFeeBase);
            startActivityForResult(intent, BitherSetting.INTENT_REF.MINER_FEE_REQUEST_CODE);
        }
    };

    private OnClickListener sendClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            baseSendClicked();
        }
    };

    private void baseSendClicked() {
        if (minerFeeMode == MinerFeeSettingActivity.MinerFeeMode.Dynamic) {
            if (!dp.isShowing()) {
                dp.show();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Long dynamicFeeBase = null;
                    try {
                        dynamicFeeBase = BitherStatsDynamicFeeApi.queryStatsDynamicFee();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    final Long finalDynamicFeeBase = dynamicFeeBase;
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            if (finalDynamicFeeBase != null && finalDynamicFeeBase > 0) {
                                sendClicked(finalDynamicFeeBase);
                            } else {
                                DialogConfirmTask confirmTask = new DialogConfirmTask(SendActivity.this, getString(R.string.dynamic_miner_fee_failure_title), new Runnable() {
                                    @Override
                                    public void run() {

                                    }
                                }, false);
                                confirmTask.setCancelable(false);
                                confirmTask.show();
                            }
                        }
                    });
                }
            }).start();
        } else if (minerFeeBase > 0) {
            sendClicked(minerFeeBase);
        }
    }

    protected void sendClicked(Long dynamicFeeBase) {
        final long btc = amountCalculatorLink.getAmount();
        if (btc > 0) {
            String address = etAddress.getText().toString().trim();
            if (Utils.validBicoinAddress(address)) {
                try {
                    CompleteTransactionRunnable completeRunnable = new
                            CompleteTransactionRunnable(addressPosition,
                            amountCalculatorLink.getAmount(), address
                            , dialogSelectChangeAddress.getChangeAddress().getAddress
                            (), new SecureCharSequence(etPassword.getText()), dynamicFeeBase);
                    completeRunnable.setHandler(completeTransactionHandler);
                    Thread thread = new Thread(completeRunnable);
                    dp.setThread(thread);
                    if (!dp.isShowing()) {
                        dp.show();
                    }
                    thread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    DropdownMessage.showDropdownMessage(SendActivity.this,
                            R.string.send_failed);
                }
            } else {
                DropdownMessage.showDropdownMessage(SendActivity.this, R.string.send_failed);
            }
        }
    }

    private TextView.OnEditorActionListener passwordAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (btnSend.isEnabled()) {
                    sendClick.onClick(btnSend);
                }
                return true;
            }
            return false;
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE) {
            final String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            new StringInputParser(input, null, isColdSendBtc) {
                @Override
                protected void bitcoinRequest(final String address, final String addressLabel,
                                              final long amount, final String bluetoothMac) {
                    if (address != null) {
                        etAddress.setText(address.toString());
                    }
                    if (amount > 0) {
                        amountCalculatorLink.setBtcAmount(amount);
                        etPassword.requestFocus();
                    } else {
                        amountCalculatorLink.requestFocus();
                    }
                    validateValues();
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs) {
                    DropdownMessage.showDropdownMessage(SendActivity.this, messageResId);
                }
            }.parse();
        } else if (requestCode == BitherSetting.INTENT_REF.MINER_FEE_REQUEST_CODE) {
            final MinerFeeSettingActivity.MinerFeeMode feeMode = (MinerFeeSettingActivity.MinerFeeMode) data.getSerializableExtra(MINER_FEE_MODE_KEY);
            final long feeBase = data.getLongExtra(MINER_FEE_BASE_KEY, 0);
            minerFeeMode = feeMode;
            minerFeeBase = feeBase;
            showMinerFee();
        }
    }

    private void showMinerFee() {
        String displayName = getString(minerFeeMode.getDisplayNameRes());
        if (minerFeeMode != MinerFeeSettingActivity.MinerFeeMode.Dynamic) {
            tvMinerFee.setText(String.format("%s %d%s", displayName, minerFeeBase / 1000, getString(R.string.send_confirm_fee_rate_symbol)));
        } else {
            tvMinerFee.setText(displayName);
        }
    }

    @Override
    public void onEntryKeyboardShow(EntryKeyboardView v) {
        vKeyboardContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEntryKeyboardHide(EntryKeyboardView v) {
        vKeyboardContainer.setVisibility(View.GONE);
    }

    @Override
    public void onSelectChangeAddress() {
        dialogSelectChangeAddress.show();
    }

    private final class ReceivingAddressListener implements OnFocusChangeListener, TextWatcher {
        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                validateValues();
            }
        }

        @Override
        public void afterTextChanged(final Editable s) {
            validateValues();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                      final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                                  final int count) {
        }
    }

    private final CurrencyAmountView.Listener amountsListener = new CurrencyAmountView.Listener() {
        @Override
        public void changed() {
            validateValues();
        }

        @Override
        public void done() {
            validateValues();
            btnSend.requestFocusFromTouch();
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (!hasFocus) {
                validateValues();
            }
        }
    };

    private TextWatcher passwordWatcher = new TextWatcher() {

        private SecureCharSequence password;

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (password != null) {
                password.wipe();
            }
            password = new SecureCharSequence(etPassword.getText());
        }

        @Override
        public void afterTextChanged(Editable s) {
            SecureCharSequence p = new SecureCharSequence(etPassword.getText());
            if (p.length() > 0) {
                if (!Utils.validPassword(p)) {
                    etPassword.setText(password);
                }
            }
            p.wipe();
            validateValues();
            password.wipe();
        }
    };

    private void validateValues() {
        boolean isValidAmounts = false;

        final BigInteger amount = BigInteger.valueOf(amountCalculatorLink.getAmount());

        if (amount == null) {
        } else if (amount.signum() > 0) {
            isValidAmounts = true;
        } else {
        }
        String address = etAddress.getText().toString().trim();
        boolean isValidAddress = Utils.validBicoinAddress(address);
        if (isColdSendBtc && isValidAddress && Utils.validBech32Address(address)) {
            isValidAddress = false;
            DropdownMessage.showDropdownMessage(this, R.string.cold_no_support_bc1_segwit_address);
            etAddress.setText("");
        }
        boolean isValidPassword = true;
        if (etPassword.getVisibility() == View.VISIBLE) {
        SecureCharSequence password = new SecureCharSequence(etPassword.getText());
            isValidPassword = Utils.validPassword(password) && password.length() >= 6 &&
                password.length() <= getResources().getInteger(R.integer.password_length_max);
            password.wipe();
        }
        btnSend.setEnabled(isValidAddress && isValidAmounts && isValidPassword);
    }

    private double getExchangeRate() {
        Ticker ticker = MarketUtil.getTickerOfDefaultMarket();
        if (ticker != null) {
            return ticker.getDefaultExchangePrice();
        }
        return 0;
    }

    private BroadcastReceiver marketBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            amountCalculatorLink.setExchangeRate(getExchangeRate());
        }
    };

    private OnClickListener optionClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogSendOption(SendActivity.this, address, SendActivity.this).show();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        amountCalculatorLink.setListener(amountsListener);
        amountCalculatorLink.setExchangeRate(getExchangeRate());
        IntentFilter marketFilter = new IntentFilter(BroadcastUtil.ACTION_MARKET);
        registerReceiver(marketBroadcastReceiver, marketFilter);
    }

    @Override
    protected void onPause() {
        amountCalculatorLink.setListener(null);
        unregisterReceiver(marketBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void processIntent() {
        isDonate = false;
        Intent intent = getIntent();
        if (intent.hasExtra(SelectAddressToSendActivity.INTENT_EXTRA_ADDRESS)) {
            String address = intent.getExtras().getString(SelectAddressToSendActivity
                    .INTENT_EXTRA_ADDRESS);
            if (Utils.validBicoinAddress(address)) {
                if (Utils.compareString(address, BitherjSettings.DONATE_ADDRESS)) {
                    isDonate = true;
                }
                etAddress.setText(address);
                long btc = intent.getExtras().getLong(SelectAddressToSendActivity
                        .INTENT_EXTRA_AMOUNT, 0);
                if (btc > 0) {
                    amountCalculatorLink.setBtcAmount(btc);
                }
                validateValues();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!kvPassword.handleBack()) {
            super.onBackPressed();
        }
    }

    private void configureDonate() {
        if (isDonate) {
            btnSend.setText(R.string.donate_sending_verb);
            tvAddressLabel.setText(R.string.donate_receiving_address_label);
            etAddress.setEnabled(false);
            etAddress.clearFocus();
            ibtnScan.setVisibility(View.GONE);
        } else {
            btnSend.setText(R.string.address_detail_send);
            tvAddressLabel.setText(R.string.send_coins_fragment_receiving_address_label);
            etAddress.setEnabled(true);
            ibtnScan.setVisibility(View.VISIBLE);
        }
    }
}
