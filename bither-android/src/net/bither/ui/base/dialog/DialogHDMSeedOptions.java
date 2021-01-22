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

package net.bither.ui.base.dialog;

import android.content.Context;

import net.bither.R;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class DialogHDMSeedOptions extends DialogWithActions {

    private DialogProgress dp;
    private HDMKeychain keychain;
    boolean isCold;

    public DialogHDMSeedOptions(Context context, HDMKeychain keychain, DialogProgress dp) {
        super(context);
        this.keychain = keychain;
        this.dp = dp;
        isCold = AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD;
    }

    @Override
    protected List<Action> getActions() {
        ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
        actions.add(new DialogWithActions.Action(isCold ? R.string.hdm_cold_seed_qr_code : R
                .string.hdm_hot_seed_qr_code, new Runnable() {
            @Override
            public void run() {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        showHDMSeedQRCode(password);
                    }
                }).show();
            }
        }));
        actions.add(new DialogWithActions.Action(isCold ? R.string.hdm_cold_seed_word_list : R
                .string.hdm_hot_seed_word_list, new Runnable() {
            @Override
            public void run() {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        showHDMSeed(password);
                    }
                }).show();
            }
        }));
        return actions;
    }


    private void showHDMSeedQRCode(SecureCharSequence password) {
        password.wipe();
        String content = keychain.getQRCodeFullEncryptPrivKey();
        new DialogSimpleQr(getContext(), content, isCold ? R.string.hdm_cold_seed_qr_code : R
                .string.hdm_hot_seed_qr_code).show();
    }

    private void showHDMSeed(final SecureCharSequence password) {
        if (!dp.isShowing()) {
            dp.show();
        }
        new Thread() {
            @Override
            public void run() {
                final ArrayList<String> words = new ArrayList<String>();
                try {
                    words.addAll(keychain.getSeedWords(password, false));
                } catch (Exception e) {
                    e.printStackTrace();
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                        }
                    });
                }
                if (words.size() > 0) {
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dp.isShowing()) {
                                dp.dismiss();
                            }
                            new DialogHDMSeedWordList(getContext(), words).show();
                        }
                    });
                }
            }
        }.start();
    }
}
