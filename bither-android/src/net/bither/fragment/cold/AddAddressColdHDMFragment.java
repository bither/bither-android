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

package net.bither.fragment.cold;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.bither.R;
import net.bither.activity.hot.AddHotAddressActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogXRandomInfo;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.BackupUtil;
import net.bither.util.KeyUtil;
import net.bither.xrandom.HDMKeychainColdUEntropyActivity;

import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by songchenwen on 15/1/7.
 */
public class AddAddressColdHDMFragment extends Fragment implements AddHotAddressActivity
        .AddAddress, IDialogPasswordListener {
    public static final String HDMSeedAddressPlaceHolder = "HDMSeedAddress";

    private Button btnAdd;
    private CheckBox cbxXRandom;
    private AddPrivateKeyActivity activity;
    private DialogProgress dp;

    private HDMKeychain chain;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_cold_hdm, container, false);
        btnAdd = (Button) v.findViewById(R.id.btn_add);
        cbxXRandom = (CheckBox) v.findViewById(R.id.cbx_xrandom);
        v.findViewById(R.id.ibtn_xrandom_info).setOnClickListener(DialogXRandomInfo.GuideClick);
        activity = (AddPrivateKeyActivity) getActivity();
        dp = new DialogProgress(activity, R.string.please_wait);
        dp.setCancelable(false);
        btnAdd.setOnClickListener(addClick);
        cbxXRandom.setOnCheckedChangeListener(xRandomCheck);
        return v;
    }

    private View.OnClickListener addClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (cbxXRandom.isChecked()) {
                final Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getActivity(), HDMKeychainColdUEntropyActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                        getActivity().startActivity(intent);
                        activity.finish();
                    }
                };
                if (AppSharedPreference.getInstance().shouldAutoShowXRandomInstruction()) {
                    DialogXRandomInfo dialog = new DialogXRandomInfo(getActivity(), true, true);
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            run.run();
                        }
                    });
                    dialog.show();
                } else {
                    run.run();
                }
            } else {
                DialogPassword dialog = new DialogPassword(getActivity(), AddAddressColdHDMFragment.this);
                dialog.show();
            }
        }
    };

    @Override
    public void onPasswordEntered(final SecureCharSequence password) {
        btnAdd.setKeepScreenOn(true);
        ThreadNeedService thread = new ThreadNeedService(dp, activity) {

            @Override
            public void runWithService(BlockchainService service) {
                chain = new HDMKeychain(new SecureRandom(), password);
                KeyUtil.setHDKeyChain(chain);
                password.wipe();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnAdd.setKeepScreenOn(false);
                        if (dp.isShowing()) {
                            dp.dismiss();
                        }
                        activity.save();
                    }
                });
            }
        };
        thread.start();
    }


    private CompoundButton.OnCheckedChangeListener xRandomCheck = new CompoundButton
            .OnCheckedChangeListener() {
        private boolean ignoreListener = false;
        private DialogConfirmTask dialog;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked && !ignoreListener) {
                cbxXRandom.setChecked(true);
                if (dialog == null) {
                    dialog = new DialogConfirmTask(getActivity(),
                            getResources().getString(R.string.xrandom_uncheck_warn), new Runnable() {
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
                dialog.show();
            }
        }
    };

    @Override
    public ArrayList<String> getAddresses() {
        ArrayList<String> as = new ArrayList<String>();
        if (chain != null) {
            as.add(HDMSeedAddressPlaceHolder);
        }
        return as;
    }
}
