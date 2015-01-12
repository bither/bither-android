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
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/1/12.
 */
public class DialogHDMSeedOptions extends DialogWithActions {

    private DialogProgress dp;
    private HDMKeychain keychain;

    public DialogHDMSeedOptions(Context context, HDMKeychain keychain, DialogProgress dp) {
        super(context);
        this.keychain = keychain;
        this.dp = dp;
    }

    @Override
    protected List<Action> getActions() {
        ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
        actions.add(new DialogWithActions.Action(R.string.hdm_cold_seed_qr_code, new Runnable() {
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
        actions.add(new DialogWithActions.Action(R.string.hdm_cold_seed_word_list, new Runnable() {
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
        String content = keychain.getEncryptedSeed();
        new DialogSimpleQr(getContext(), content, R.string.hdm_cold_seed_qr_code).show();
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
                    words.addAll(keychain.getSeedWords(password));
                } catch (MnemonicException.MnemonicLengthException e) {
                    e.printStackTrace();
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
