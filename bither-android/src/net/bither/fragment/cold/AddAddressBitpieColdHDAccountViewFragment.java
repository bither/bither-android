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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.BitpieHDAccountCold;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.ui.base.dialog.DialogBitpieColdInfo;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogHDMSeedWordList;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSimpleQr;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yiwenlong on 19/1/15.
 */
public class AddAddressBitpieColdHDAccountViewFragment extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_address_hot_hd_account_view, container,
                false);
        TextView label = v.findViewById(R.id.tv_account_label);
        label.setText(R.string.bitpie_add_hd_account_view_label);
        TextView btnQr = v.findViewById(R.id.btn_qr);
        btnQr.setText(R.string.bitpie_add_hd_account_seed_qr_code);
        btnQr.setVisibility(View.VISIBLE);
        TextView btnPhrase = v.findViewById(R.id.btn_phrase);
        btnPhrase.setText(R.string.bitpie_add_hd_account_seed_qr_phrase);
        ImageButton ibtnInfo = v.findViewById(R.id.ibtn_info);
        ibtnInfo.setOnClickListener(bitpieColdInfoClick);
        ibtnInfo.setVisibility(View.VISIBLE);
        btnQr.setOnClickListener(this);
        btnPhrase.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_qr:
                showQr();
                return;
            case R.id.btn_phrase:
                showPhrase();
                return;
        }
    }

    private void showQr() {
        final BitpieHDAccountCold account = AddressManager.getInstance().getBitpieHDAccountCold();
        if (account == null) {
            return;
        }
        new DialogPassword(getActivity(), new IDialogPasswordListener() {
            @Override
            public void onPasswordEntered(final SecureCharSequence password) {
                password.wipe();
                String content = account.getQRCodeFullEncryptPrivKey();
                new DialogSimpleQr(getActivity(), content, R.string.add_hd_account_seed_qr_code)
                        .show();
            }
        }).show();
    }

    private void showPhrase() {
        final BitpieHDAccountCold account = AddressManager.getInstance().getBitpieHDAccountCold();
        if (account == null) {
            return;
        }
        new DialogPassword(getActivity(), new IDialogPasswordListener() {
            @Override
            public void onPasswordEntered(final SecureCharSequence password) {
                final DialogProgress dp = new DialogProgress(getActivity(), R.string.please_wait);
                dp.setCancelable(false);
                dp.show();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            final List<String> words = account.getSeedWords(password);
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    new DialogHDMSeedWordList(getActivity(), words).show();
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (dp.isShowing()) {
                                        dp.dismiss();
                                    }
                                    DialogConfirmTask dialog = new DialogConfirmTask(getActivity(), getString(R.string.hd_account_valid_failed), null, false);
                                    dialog.setCancelable(false);
                                    dialog.show();
                                }
                            });
                        } finally {
                            password.wipe();
                        }
                    }
                }.start();
            }
        }).show();
    }

    private View.OnClickListener bitpieColdInfoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogBitpieColdInfo(getActivity()).show();
        }
    };

}
