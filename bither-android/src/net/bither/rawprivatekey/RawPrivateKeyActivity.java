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

package net.bither.rawprivatekey;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.crypto.DumpedPrivateKey;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BackupUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

/**
 * Created by songchenwen on 14/12/4.
 */
public class RawPrivateKeyActivity extends SwipeRightActivity implements IDialogPasswordListener {
    private RawDataView vData;
    private Button btnZero;
    private Button btnOne;
    private Button btnAdd;
    private TextView tvPrivateKey;
    private TextView tvAddress;
    private LinearLayout llShow;
    private LinearLayout llInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_add_raw_private_key);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        vData = (RawDataView) findViewById(R.id.v_data);
        btnZero = (Button) findViewById(R.id.btn_zero);
        btnZero.setOnClickListener(addDataClick);
        btnOne = (Button) findViewById(R.id.btn_one);
        btnOne.setOnClickListener(addDataClick);
        llShow = (LinearLayout) findViewById(R.id.ll_show);
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        tvPrivateKey = (TextView) findViewById(R.id.tv_private_key);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        btnAdd = (Button) findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(addKeyClick);
        vData.setRestrictedSize(getResources().getDisplayMetrics().widthPixels - UIUtil.dip2pix
                (16), (int) (getResources().getDisplayMetrics().heightPixels * 0.47f));
        vData.setDataSize(16, 16);
        llShow.setVisibility(View.GONE);
        llInput.setVisibility(View.VISIBLE);
    }

    private void handleData() {
        byte[] data = vData.getData();
        if (data == null) {
            return;
        }
        if (!checkValue(data)) {
            DropdownMessage.showDropdownMessage(this, R.string.raw_private_key_not_safe,
                    new Runnable() {
                @Override
                public void run() {
                    vData.setDataSize(16, 16);
                }
            });
            return;
        }
        BigInteger value = new BigInteger(1, data);
        value = value.mod(ECKey.CURVE.getN());

        ECKey key = new ECKey(value);
        String address = Utils.toAddress(key.getPubKeyHash());
        SecureCharSequence privateKey = new DumpedPrivateKey(key.getPrivKeyBytes(),
                true).toSecureCharSequence();
        key.clearPrivateKey();
        tvPrivateKey.setText(WalletUtils.formatHashFromCharSequence(privateKey, 4, 16));
        tvAddress.setText(WalletUtils.formatHash(address, 4, 12));
        llInput.setVisibility(View.GONE);
        llShow.setVisibility(View.VISIBLE);
    }

    private boolean checkValue(byte[] data) {
        BigInteger value = new BigInteger(1, data);
        if (value.compareTo(BigInteger.ZERO) == 0 || value.compareTo(ECKey.CURVE.getN()) == 0) {
            return false;
        }
        return true;
    }

    private View.OnClickListener addDataClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (vData.dataLength() > 0 && vData.filledDataLength() < vData.dataLength()) {
                vData.addData(v == btnOne);
                if (vData.filledDataLength() == vData.dataLength()) {
                    handleData();
                    return;
                }
//                if (vData.testNextOneValue().compareTo(max) >= 0) {
//                    btnOne.setVisibility(View.GONE);
//                }
//                if (vData.testNextZeroValue().compareTo(min) <= 0) {
//                    btnZero.setVisibility(View.GONE);
//                }
            }
        }
    };

    private View.OnClickListener addKeyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogPassword(RawPrivateKeyActivity.this, RawPrivateKeyActivity.this).show();
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        DialogProgress dp = new DialogProgress(this, R.string.please_wait);
        dp.show();
        new ThreadNeedService(dp, this) {
            @Override
            public void runWithService(BlockchainService service) {
                if (service != null) {
                    service.stopAndUnregister();
                }
                byte[] data = vData.getData();
                ECKey key = new ECKey(data, null);
                key = PrivateKeyUtil.encrypt(key, password);
                key.clearPrivateKey();
                Utils.wipeBytes(data);
                password.wipe();
                Address address = new Address(key.toAddress(), key.getPubKey(),
                        PrivateKeyUtil.getEncryptedString(key), key.isFromXRandom());
                key.clearPrivateKey();
                AddressManager.getInstance().addAddress(address);
                if (AppSharedPreference.getInstance().getPasswordSeed() == null) {
                    PasswordSeed passwordSeed = new PasswordSeed(address);
                    AppSharedPreference.getInstance().setPasswordSeed(passwordSeed);
                }
                if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode
                        .COLD) {
                    BackupUtil.backupColdKey(false);
                } else {
                    BackupUtil.backupHotKey();
                }
                if (service != null) {
                    service.startAndRegister();
                }
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings
                                .AppMode.HOT) {
                            Fragment f = BitherApplication.hotActivity.getFragmentAtIndex(1);
                            if (f instanceof HotAddressFragment) {
                                HotAddressFragment hotAddressFragment = (HotAddressFragment) f;
                                hotAddressFragment.refresh();
                            }
                        } else {
                            Fragment f = BitherApplication.coldActivity.getFragmentAtIndex(1);
                            if (f instanceof ColdAddressFragment) {
                                ColdAddressFragment coldAddressFragment = (ColdAddressFragment) f;
                                coldAddressFragment.refresh();
                            }
                        }
                    }
                });
            }
        }.start();
    }
}
