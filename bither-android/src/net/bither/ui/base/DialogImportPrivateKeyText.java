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
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.params.MainNetParams;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.activity.cold.ColdActivity;
import net.bither.activity.hot.HotActivity;
import net.bither.activity.hot.HotAdvanceActivity;
import net.bither.fragment.Refreshable;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.runnable.CheckAddressRunnable;
import net.bither.runnable.HandlerMessage;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.util.PrivateKeyUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.WalletUtils;

import java.util.ArrayList;
import java.util.List;

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
    private ProgressDialog pd;

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
            tvError.setText(R.string.import_private_key_text_format_erro);
            if (StringUtil.isEmpty(s)) {
                tvError.setVisibility(View.VISIBLE);
                shake();
                return;
            } else if (!StringUtil.validBitcoinPrivateKey(s)) {
                tvError.setVisibility(View.VISIBLE);
                shake();
                return;
            }
            try {
                ECKey key = new DumpedPrivateKey(MainNetParams.get(), s).getKey();
                if (!key.isCompressed()) {
                    tvError.setText(R.string.only_supports_the_compressed_private_key);
                    tvError.setVisibility(View.VISIBLE);
                    shake();
                    return;

                }
                Address address = key.toAddress(BitherSetting.NETWORK_PARAMETERS);
                CheckAddressRunnable checkAddressRunnable = new CheckAddressRunnable(address.toString());
                checkAddressRunnable.setHandler(checkAddressHandler);
                new Thread(checkAddressRunnable).start();
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }
        } else {
            dismiss();
        }
    }

    Handler checkAddressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerMessage.MSG_PREPARE:
                    if (pd == null) {
                        pd = new ProgressDialog(activity, activity.getString(R.string.please_wait), null);
                    }
                    pd.show();
                    break;
                case HandlerMessage.MSG_SUCCESS:
                    if (pd != null) {
                        pd.dismiss();
                    }
                    boolean isCheck = Boolean.valueOf(msg.obj.toString());
                    if (isCheck) {
                        privateKeyString = et.getText().toString();
                        dismiss();

                    } else {
                        DropdownMessage.showDropdownMessage(activity, R.string.please_wait);
                    }
                    break;
                case HandlerMessage.MSG_FAILURE:
                    if (pd != null) {
                        pd.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity, R.string.network_or_connection_error);
                    break;
            }
        }
    };

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

        private void dpDismissWithError(final int stringId) {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    DropdownMessage.showDropdownMessage(activity, stringId
                    );
                }
            });

        }

        @Override
        public void runWithService(BlockchainService service) {
            ECKey key = PrivateKeyUtil.getEncryptedECKey(privateKey, password);
            if (key == null) {
                dpDismissWithError(R.string.import_private_key_qr_code_failed);
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
                List<BitherAddressWithPrivateKey> wallets = new ArrayList<BitherAddressWithPrivateKey>();
                wallets.add(wallet);
                WalletUtils.addAddressWithPrivateKey(service, wallets);
            }

            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (dp != null && dp.isShowing()) {
                        dp.setThread(null);
                        dp.dismiss();
                    }
                    if (activity instanceof HotAdvanceActivity) {
                        ((HotAdvanceActivity) activity).showImportSuccess();
                    }
                    if (activity instanceof ColdActivity) {
                        ColdActivity a = (ColdActivity) activity;
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
