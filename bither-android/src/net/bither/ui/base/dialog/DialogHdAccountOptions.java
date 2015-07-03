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

import android.app.Activity;

import net.bither.R;
import net.bither.activity.hot.HDAccountDetailActivity;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/4/18.
 */
public class DialogHdAccountOptions extends DialogWithActions {
    private HDAccount account;
    private boolean fromDetail;
    private Activity activity;

    public DialogHdAccountOptions(Activity context, HDAccount account) {
        super(context);
        this.account = account;
        activity = context;
        fromDetail = context instanceof HDAccountDetailActivity;
    }

    @Override
    protected List<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        if (fromDetail) {
            actions.add(new Action(R.string.hd_account_old_addresses, new Runnable() {
                @Override
                public void run() {
                    if (account.issuedExternalIndex() < 0) {
                        DropdownMessage.showDropdownMessage(activity, R.string
                                .hd_account_old_addresses_zero);
                        return;
                    }
                    new DialogHdOldAddresses(activity, account).show();
                }
            }));
        }
        actions.add(new Action(R.string.add_hd_account_seed_qr_code, new Runnable() {
            @Override
            public void run() {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(SecureCharSequence password) {
                        password.wipe();
                        String content = account.getQRCodeFullEncryptPrivKey();
                        new DialogSimpleQr(getContext(), content, R.string
                                .add_hd_account_seed_qr_code).show();
                    }
                }).show();
            }
        }));
        actions.add(new Action(R.string.add_hd_account_seed_qr_phrase, new Runnable() {
            @Override
            public void run() {
                new DialogPassword(getContext(), new IDialogPasswordListener() {
                    @Override
                    public void onPasswordEntered(final SecureCharSequence password) {
                        final DialogProgress dp = new DialogProgress(getContext(), R.string
                                .please_wait);
                        dp.setCancelable(false);
                        dp.show();
                        new Thread() {
                            @Override
                            public void run() {
                                final ArrayList<String> words = new ArrayList<String>();
                                try {
                                    words.addAll(account.getSeedWords(password));
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
                                } finally {
                                    password.wipe();
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
                }).show();
            }
        }));
        return actions;
    }
}
