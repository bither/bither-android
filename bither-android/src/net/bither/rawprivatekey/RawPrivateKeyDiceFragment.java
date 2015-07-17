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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.crypto.DumpedPrivateKey;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.cold.ColdAddressFragment;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BackupUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

/**
 * Created by songchenwen on 15/2/15.
 */
public class RawPrivateKeyDiceFragment extends Fragment implements IDialogPasswordListener {
    private RawDataDiceView vData;
    private Button btnAdd;
    private TextView tvPrivateKey;
    private TextView tvAddress;
    private LinearLayout llShow;
    private LinearLayout llInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_raw_private_key_dice, null);
        vData = (RawDataDiceView) v.findViewById(R.id.v_data);
        v.findViewById(R.id.btn_one).setOnClickListener(addDataClick);
        v.findViewById(R.id.btn_two).setOnClickListener(addDataClick);
        v.findViewById(R.id.btn_three).setOnClickListener(addDataClick);
        v.findViewById(R.id.btn_four).setOnClickListener(addDataClick);
        v.findViewById(R.id.btn_five).setOnClickListener(addDataClick);
        v.findViewById(R.id.btn_six).setOnClickListener(addDataClick);
        v.findViewById(R.id.ibtn_delete).setOnClickListener(deleteClick);
        v.findViewById(R.id.btn_clear).setOnClickListener(clearClick);
        llShow = (LinearLayout) v.findViewById(R.id.ll_show);
        llInput = (LinearLayout) v.findViewById(R.id.ll_input);
        tvPrivateKey = (TextView) v.findViewById(R.id.tv_private_key);
        tvAddress = (TextView) v.findViewById(R.id.tv_address);
        btnAdd = (Button) v.findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(addKeyClick);
        int maxSize = Math.min(UIUtil.getScreenWidth() - UIUtil.dip2pix(16),
                UIUtil.getScreenHeight() - UIUtil.dip2pix(241));
        vData.setRestrictedSize(maxSize, maxSize);
        vData.setDataSize(10, 10);
        llShow.setVisibility(View.GONE);
        llInput.setVisibility(View.VISIBLE);
        return v;
    }


    private void handleData() {
        final DialogProgress dp = new DialogProgress(getActivity(), R.string.please_wait);
        dp.show();
        new Thread() {
            @Override
            public void run() {
                final byte[] data = vData.getData();
                if (data == null) {
                    return;
                }
                if (!checkValue(data)) {
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.dismiss();
                            DropdownMessage.showDropdownMessage(getActivity(),
                                    R.string.raw_private_key_not_safe, new Runnable() {
                                        @Override
                                        public void run() {
                                            vData.setDataSize(10, 10);
                                        }
                                    });
                        }
                    });
                    return;
                }
                BigInteger value = new BigInteger(1, data);
                value = value.mod(ECKey.CURVE.getN());

                ECKey key = new ECKey(value, null, true);
                final String address = Utils.toAddress(key.getPubKeyHash());
                final SecureCharSequence privateKey = new DumpedPrivateKey(key.getPrivKeyBytes(),
                        true).toSecureCharSequence();
                Utils.wipeBytes(data);
                key.clearPrivateKey();
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        dp.dismiss();
                        tvPrivateKey.setText(Utils.formatHashFromCharSequence(privateKey,
                                4, 16));
                        tvAddress.setText(WalletUtils.formatHash(address, 4, 12));
                        llInput.setVisibility(View.GONE);
                        llShow.setVisibility(View.VISIBLE);
                        privateKey.wipe();
                    }
                });
            }
        }.start();
    }

    private boolean checkValue(byte[] data) {
        BigInteger value = new BigInteger(1, data).mod(ECKey.CURVE.getN());
        if (value.compareTo(BigInteger.ZERO) == 0) {
            return false;
        }
        return true;
    }

    private View.OnClickListener addDataClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (vData.dataLength() > 0 && vData.filledDataLength() < vData.dataLength()) {
                RawDataDiceView.Dice d = null;
                switch (v.getId()) {
                    case R.id.btn_one:
                        d = RawDataDiceView.Dice.One;
                        break;
                    case R.id.btn_two:
                        d = RawDataDiceView.Dice.Two;
                        break;
                    case R.id.btn_three:
                        d = RawDataDiceView.Dice.Three;
                        break;
                    case R.id.btn_four:
                        d = RawDataDiceView.Dice.Four;
                        break;
                    case R.id.btn_five:
                        d = RawDataDiceView.Dice.Five;
                        break;
                    case R.id.btn_six:
                        d = RawDataDiceView.Dice.Six;
                        break;
                }
                if (d == null) {
                    return;
                }
                vData.addData(d);
                if (vData.filledDataLength() == vData.dataLength()) {
                    handleData();
                    return;
                }
            }
        }
    };

    private View.OnClickListener deleteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            vData.deleteLast();
        }
    };

    private View.OnClickListener clearClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            vData.removeAllData();
        }
    };

    private View.OnClickListener addKeyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogPassword(getActivity(), RawPrivateKeyDiceFragment.this).show();
        }
    };

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        DialogProgress dp = new DialogProgress(getActivity(), R.string.please_wait);
        dp.show();
        new ThreadNeedService(dp, getActivity()) {
            @Override
            public void runWithService(BlockchainService service) {
                if (service != null) {
                    service.stopAndUnregister();
                }
                byte[] data = vData.getData();
                vData.clearData();
                BigInteger value = new BigInteger(1, data);
                value = value.mod(ECKey.CURVE.getN());
                ECKey key = new ECKey(value, null, true);
                key = PrivateKeyUtil.encrypt(key, password);
                Utils.wipeBytes(data);
                password.wipe();
                Address address = new Address(key.toAddress(), key.getPubKey(),
                        PrivateKeyUtil.getEncryptedString(key), false, false);
                key.clearPrivateKey();
                AddressManager.getInstance().addAddress(address);

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
                        getActivity().finish();
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
