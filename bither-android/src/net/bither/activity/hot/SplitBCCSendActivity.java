package net.bither.activity.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.api.BccBroadCastApi;
import net.bither.bitherj.api.BccHasAddressApi;
import net.bither.bitherj.api.GetBcdBlockHashApi;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Out;
import net.bither.bitherj.core.SplitCoin;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.utils.UnitUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.qrcode.ScanActivity;
import net.bither.runnable.BaseRunnable;
import net.bither.runnable.CommitTransactionThread;
import net.bither.runnable.CompleteTransactionRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.runnable.RCheckRunnable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogHdSendConfirm;
import net.bither.ui.base.dialog.DialogRCheck;
import net.bither.ui.base.keyboard.EntryKeyboardView;
import net.bither.ui.base.keyboard.password.PasswordEntryKeyboardView;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.InputParser;
import net.bither.util.LogUtil;

import org.json.JSONObject;

import java.util.List;

import static net.bither.activity.hot.HotAdvanceActivity.SplitCoinKey;

/**
 * Created by ltq on 2017/7/26.
 */

public class SplitBCCSendActivity extends SwipeRightActivity implements EntryKeyboardView
        .EntryKeyboardViewListener {
    protected int addressPosition;
    protected Address address;

    protected TextView tvBalance;
    protected TextView tvAddressLabel;
    protected EditText etAddress;
    protected EditText etPassword;
    private String toAddress;
    private ImageButton ibtnScan;
    protected Button btnSend;
    protected DialogRCheck dp;
    private PasswordEntryKeyboardView kvPassword;
    private View vKeyboardContainer;
    private List<Tx> txs;
    private long btcAmount;
    protected TextView tvSplitCoinName;
    protected SplitCoin splitCoin = SplitCoin.BCC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_scan_address_to_split_bcc);
        Intent intent = getIntent();
        splitCoin = (SplitCoin) intent.getSerializableExtra(SplitCoinKey);
        initAddress();
        initView();
        initBalance();
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
        findViewById(R.id.ibtn_cancel).setOnClickListener(
                new IBackClickListener(0, R.anim.slide_out_right));
        tvBalance = (TextView) findViewById(R.id.tv_balance);
        tvAddressLabel = (TextView) findViewById(R.id.tv_address_label);
        etAddress = (EditText) findViewById(R.id.et_address);
        ibtnScan = (ImageButton) findViewById(R.id.ibtn_scan);
        btnSend = (Button) findViewById(R.id.btn_send);
        etPassword = (EditText) findViewById(R.id.et_password);
        kvPassword = (PasswordEntryKeyboardView) findViewById(R.id.kv_password);
        tvSplitCoinName = (TextView) findViewById(R.id.tv_split_coin_name);
        vKeyboardContainer = findViewById(R.id.v_keyboard_container);
        etPassword.addTextChangedListener(passwordWatcher);
        etPassword.setOnEditorActionListener(passwordAction);
        SplitBCCSendActivity.ReceivingAddressListener addressListener = new SplitBCCSendActivity.ReceivingAddressListener();
        etAddress.setOnFocusChangeListener(addressListener);
        etAddress.addTextChangedListener(addressListener);
        dp = new DialogRCheck(this);
        ibtnScan.setOnClickListener(scanClick);
        btnSend.setOnClickListener(sendClick);
        kvPassword.registerEditText(etPassword).setListener(this);
        tvSplitCoinName.setText(splitCoin.getName().toString());
        etAddress.setHint(Utils.format(getString(R.string.bitpie_split_coin_address), splitCoin.getName()));
    }

    protected void initBalance() {
        tvBalance.setText(UnitUtil.formatValue(getAmount(AbstractDb.txProvider.
                        getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(), address.getAddress())),
                splitCoin.getBitcoinUnit()));
    }

    private View.OnClickListener scanClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SplitBCCSendActivity.this, ScanActivity.class);
            startActivityForResult(intent, BitherSetting.INTENT_REF.SCAN_REQUEST_CODE);
        }
    };

    private DialogHdSendConfirm.SendConfirmListener sendConfirmListener = new DialogHdSendConfirm.SendConfirmListener() {

        @Override
        public void onConfirm() {
            if (!dp.isShowing()) {
                dp.show();
            }

            new Thread() {
                @Override
                public void run() {
                    String errorMsg = null;
                    for (final Tx tx : txs) {
                        try {
                            String raw = Utils.bytesToHexString(tx.bitcoinSerialize());
                            BccBroadCastApi bccBroadCastApi = new BccBroadCastApi(raw, splitCoin);
                            bccBroadCastApi.handleHttpPost();
                            JSONObject jsonObject = new JSONObject(bccBroadCastApi.getResult());
                            boolean result = jsonObject.getInt("result") == 1 ? true : false;
                            if (!result) {
                                final JSONObject jsonObj = jsonObject.getJSONObject("error");
                                final int code = jsonObj.getInt("code");
                                final String message = jsonObj.getString("message");
                                errorMsg = String.valueOf(code) + message;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorMsg = getString(R.string.send_failed);
                        }

                        if (errorMsg != null) {
                            break;
                        }
                    }

                    if (errorMsg == null) {
                        saveIsObtainBcc();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp.isShowing()) {
                                    dp.dismiss();
                                }
                                DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, R.string.send_success);
                            }
                        });
                        btnSend.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setResult(SplitBccSelectAddressActivity.
                                        SPLIT_BCC_HDACCOUNT_REQUEST_CODE, null);
                                finish();
                            }
                        }, 1000);
                    } else {
                        final String finalErrorMsg = errorMsg;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp.isShowing()) {
                                    dp.dismiss();
                                }
                                DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, finalErrorMsg);
                                btnSend.setEnabled(true);
                            }
                        });
                    }
                }
            }.start();
        }

        @Override
        public void onCancel() {

        }
    };

    private View.OnClickListener sendClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            sendClicked();
        }
    };

    private Handler completeTransactionHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_SUCCESS:
                    if (!dp.isShowing()) {
                        dp.show();
                    }
                    if (msg.obj != null && msg.obj instanceof List) {
                        txs = (List<Tx>) msg.obj;
                        RCheckRunnable run = new RCheckRunnable(address, txs);
                        run.setHandler(rcheckHandler);
                        new Thread(run).start();
                    } else {
                        DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this,
                                R.string.password_wrong);
                    }
                    break;
                case HandlerMessage.MSG_PASSWORD_WRONG:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, R.string.password_wrong);
                    break;
                case HandlerMessage.MSG_FAILURE:
                    if (dp.isShowing()) {
                        dp.dismiss();
                    }
                    String msgError = getString(R.string.send_failed);
                    if (msg.obj instanceof String) {
                        msgError = (String) msg.obj;
                    }
                    DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, msgError);
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
                    if (msg.obj != null && msg.obj instanceof List) {
                        final List<Tx> txs = (List<Tx>) msg.obj;
                        if (!dp.isShowing()) {
                            dp.show();
                        }
                        tvAddressLabel.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                new DialogHdSendConfirm(SplitBCCSendActivity.this, toAddress, txs, sendConfirmListener, splitCoin).show();
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

    protected void sendClicked() {
        if (!dp.isShowing()) {
            dp.show();
        }
        BaseRunnable baseRunnable = new BaseRunnable() {
            @Override
            public void run() {
                BccHasAddressApi bccHasAddressApi = new BccHasAddressApi(toAddress, splitCoin);
                try {
                    bccHasAddressApi.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(bccHasAddressApi
                            .getResult());
                    boolean result = jsonObject.getInt("result") == 1 ? true : false;
                    if (result) {
                        if(splitCoin == SplitCoin.BCD) {
                            getPreBlockHash();
                        }else {
                            send();
                        }
                    } else {
                        DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, Utils.format(getString(R.string.not_bitpie_split_coin_address), splitCoin.getName()));
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(baseRunnable).start();
    }

    private void getPreBlockHash() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GetBcdBlockHashApi getBlockHash = new GetBcdBlockHashApi();

                    getBlockHash.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(getBlockHash
                            .getResult());
                    String blockHash = jsonObject.getString("current_block_hash");
                    if(blockHash != null && !blockHash.isEmpty()) {
                        send(blockHash);
                    }else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dp.isShowing()) {
                                    dp.dismiss();
                                }
                                DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, R.string.get_bcd_block_hash_error);
                            }
                        });
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this, R.string.get_bcd_block_hash_error);
                        }
                    });
                }
            }
        }).start();
    }

    private void send(String...blockHash) {
        try {
            CompleteTransactionRunnable completeRunnable = new
                    CompleteTransactionRunnable(splitCoin.getCoin(), addressPosition, btcAmount
                    , toAddress, toAddress, new SecureCharSequence(etPassword.getText()));
            completeRunnable.setHandler(completeTransactionHandler);
            if(blockHash != null && blockHash.length > 0) {
                completeRunnable.setBlockHash(blockHash[0]);
            }
            Thread thread = new Thread(completeRunnable);
            dp.setThread(thread);
            if (!dp.isShowing()) {
                dp.show();
            }
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this,
                    R.string.send_failed);
        }
    }

    private final class ReceivingAddressListener implements View.OnFocusChangeListener, TextWatcher {
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

    protected void validateValues() {
        boolean isValidAmounts = false;
        btcAmount = getAmount(AbstractDb.txProvider.getUnspentOutputByBlockNo(splitCoin.getForkBlockHeight(), address.getAddress()));
        if (btcAmount > 0) {
            isValidAmounts = true;
        }
        toAddress = etAddress.getText().toString().trim();
        boolean isValidAddress = Utils.validSplitBitCoinAddress(toAddress,splitCoin);
        boolean isValidPassword = true;
        if (etPassword.getVisibility() == View.VISIBLE) {
            SecureCharSequence password = new SecureCharSequence(etPassword.getText());
            isValidPassword = Utils.validPassword(password) && password.length() >= 6 &&
                    password.length() <= getResources().getInteger(R.integer.password_length_max);
            password.wipe();
        }
        btnSend.setEnabled(isValidAddress && isValidAmounts && isValidPassword);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BitherSetting.INTENT_REF.SCAN_REQUEST_CODE && resultCode == Activity
                .RESULT_OK) {
            final String input = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            new InputParser.StringInputParser(input, splitCoin, false) {
                @Override
                protected void bitcoinRequest(final String address, final String addressLabel,
                                              final long amount, final String bluetoothMac) {
                    etAddress.setText(address.toString());
                    if (amount > 0) {
                        etPassword.requestFocus();
                    } else {
                    }
                    validateValues();
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs) {
                    DropdownMessage.showDropdownMessage(SplitBCCSendActivity.this,
                            Utils.format(getString(R.string.not_bitpie_split_coin_address), splitCoin.getName()));
                }
            }.parse();

        }

    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    public void onEntryKeyboardHide(EntryKeyboardView v) {
        vKeyboardContainer.setVisibility(View.GONE);

    }

    @Override
    public void onEntryKeyboardShow(EntryKeyboardView v) {
        vKeyboardContainer.setVisibility(View.VISIBLE);
    }

    public long getAmount(List<Out> outs) {
        long amount = 0;
        for (Out out : outs) {
            amount += out.getOutValue();
        }
        return amount;
    }

    void saveIsObtainBcc() {
        AppSharedPreference.getInstance().setIsObtainBcc(address.getAddress() + splitCoin.getIsGatKey(), true);
    }
}
