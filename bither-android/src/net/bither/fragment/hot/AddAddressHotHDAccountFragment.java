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

package net.bither.fragment.hot;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.bither.R;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogXRandomInfo;
import net.bither.util.ThreadUtil;

import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by songchenwen on 15/4/16.
 */
public class AddAddressHotHDAccountFragment extends Fragment implements AddHotAddressActivity
        .AddAddress {
    public static final String HDAccountPlaceHolder = "HDAccount";
    private CheckBox cbxXRandom;
    private DialogProgress dp;
    private HDAccount hdAccount;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_hot_hd_account, container, false);
        cbxXRandom = (CheckBox) v.findViewById(R.id.cbx_xrandom);
        cbxXRandom.setOnCheckedChangeListener(xRandomCheck);
        v.findViewById(R.id.ibtn_xrandom_info).setOnClickListener(DialogXRandomInfo.GuideClick);
        v.findViewById(R.id.btn_add).setOnClickListener(addClick);
        dp = new DialogProgress(v.getContext(), R.string.please_wait);
        return v;
    }

    private View.OnClickListener addClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (cbxXRandom.isChecked()) {

            } else {
                final DialogPassword.PasswordGetter passwordGetter = new DialogPassword
                        .PasswordGetter(getActivity());
                new ThreadNeedService(null, getActivity()) {
                    @Override
                    public void runWithService(BlockchainService service) {
                        SecureCharSequence password = passwordGetter.getPassword();
                        if (password == null) {
                            return;
                        }
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                v.setKeepScreenOn(true);
                                AddAddressHotHDAccountFragment.this.dp.show();
                            }
                        });
                        hdAccount = new HDAccount(new SecureRandom(), password);
                        //TODO add this account to address manager
                        password.wipe();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                v.setKeepScreenOn(false);
                                AddAddressHotHDAccountFragment.this.dp.dismiss();
                                if (getActivity() instanceof AddPrivateKeyActivity) {
                                    AddPrivateKeyActivity activity = (AddPrivateKeyActivity)
                                            getActivity();
                                    activity.save();
                                } else {
                                    getActivity().finish();
                                }
                            }
                        });
                    }
                }.start();
            }
        }
    };

    @Override
    public ArrayList<String> getAddresses() {
        ArrayList<String> addresses = new ArrayList<String>();
        if (hdAccount != null) {
            addresses.add(HDAccountPlaceHolder);
        }
        return addresses;
    }

    private CompoundButton.OnCheckedChangeListener xRandomCheck = new CompoundButton
            .OnCheckedChangeListener() {
        private boolean ignoreListener = false;
        private DialogConfirmTask dialog;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked && !ignoreListener) {
                cbxXRandom.setChecked(true);
                getDialog().show();
            }
        }

        private DialogConfirmTask getDialog() {
            if (dialog == null) {
                dialog = new DialogConfirmTask(getActivity(), getResources().getString(R.string
                        .xrandom_uncheck_warn), new Runnable() {
                    @Override
                    public void run() {
                        cbxXRandom.post(new Runnable() {
                            @Override
                            public void run() {
                                ignoreListener = true;
                                cbxXRandom.setChecked(false);
                                ignoreListener = false;
                            }
                        });
                    }
                });
            }
            return dialog;
        }
    };

}
