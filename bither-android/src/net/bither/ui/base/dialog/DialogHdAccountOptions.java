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
import net.bither.activity.hot.AddressDetailActivity;
import net.bither.activity.hot.HDAccountDetailActivity;
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.db.AbstractDb;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.DetectAnotherAssetsUtil;
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
    private boolean isSegwitAddress;
    private AbstractHD.PathType pathType;

    public DialogHdAccountOptions(Activity context, HDAccount account, boolean isSwitchToSegwit) {
        super(context);
        this.account = account;
        activity = context;
        fromDetail = context instanceof HDAccountDetailActivity;
        this.isSegwitAddress = isSwitchToSegwit;
        if (isSwitchToSegwit) {
            pathType = AbstractHD.PathType.EXTERNAL_BIP49_PATH;
        } else {
            pathType = AbstractHD.PathType.EXTERNAL_ROOT_PATH;
        }
    }

    @Override
    protected List<Action> getActions() {
        final ArrayList<Action> actions = new ArrayList<>();
        if (fromDetail) {
            if (isSegwitAddress || (!isSegwitAddress && AbstractDb.hdAccountProvider.getSegwitExternalPub(account.getHdSeedId()) != null)) {
                actions.add(new Action(!isSegwitAddress ? R.string.address_segwit : R.string.address_normal, new Runnable() {
                    @Override
                    public void run() {
                        final DialogProgress dp = new DialogProgress(activity, R.string.please_wait);
                        dp.setCancelable(false);
                        dp.show();
                        new Thread() {
                            @Override
                            public void run() {
                                ThreadUtil.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp.dismiss();
                                            ((AddressDetailActivity) activity).isSegwitAddress =
                                                    !isSegwitAddress;
                                            ((HDAccountDetailActivity) activity).loadData();
                                    }
                                });
                            }
                        }.start();
                    }
                }));
            }
            actions.add(new Action(R.string.hd_account_request_new_receiving_address, new
                    Runnable() {
                @Override
                public void run() {
                    final DialogProgress dp = new DialogProgress(activity, R.string.please_wait);
                    dp.setCancelable(false);
                    dp.show();
                    new Thread() {
                        @Override
                        public void run() {
                            final boolean result = account.requestNewReceivingAddress(pathType);
                            ThreadUtil.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    dp.dismiss();
                                    if (result) {
                                        ((HDAccountDetailActivity) activity).loadData();
                                    } else {
                                        DropdownMessage.showDropdownMessage(activity, R.string
                                                .hd_account_request_new_receiving_address_failed);
                                    }
                                }
                            });
                        }
                    }.start();
                }
            }));
            actions.add(new Action(R.string.hd_account_old_addresses, new Runnable() {
                @Override
                public void run() {
                    if (account.issuedExternalIndex(pathType) < 0) {
                        DropdownMessage.showDropdownMessage(activity, R.string
                                .hd_account_old_addresses_zero);
                        return;
                    }
                    new DialogHdAccountOldAddresses(activity, account, pathType).show();
                }
            }));
        }
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
        actions.add(new DialogWithActions.Action(R.string.add_cold_hd_account_xpub_b58, new
                Runnable() {
                    @Override
                    public void run() {
                        new DialogPassword(getContext(), new IDialogPasswordListener() {
                            @Override
                            public void onPasswordEntered(final SecureCharSequence password) {
                                final DialogProgress dp = new DialogProgress(getContext(), R.string
                                        .please_wait);
                                dp.show();
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            final String xpub = account.xPubB58(password,
                                                    AbstractHD.PurposePathLevel.P2SHP2WPKH);
                                            ThreadUtil.runOnMainThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dp.dismiss();
                                                    new DialogSimpleQr(getContext(), xpub, R.string
                                                            .add_cold_hd_account_xpub_b58).show();
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            ThreadUtil.runOnMainThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dp.dismiss();
                                                }
                                            });
                                        }
                                    }
                                }.start();
                            }
                        }).show();
                    }
                }));
        if (!isSegwitAddress) {
            actions.add(new Action(R.string.detect_another_BCC_assets, new Runnable() {
                @Override
                public void run() {
                    if (!account.isSyncComplete()) {
                        DropdownMessage.showDropdownMessage(activity, R.string.no_sync_complete);
                    } else {
                        DetectAnotherAssetsUtil detectUtil = new DetectAnotherAssetsUtil(activity);
                        detectUtil.getBCCHDUnspentOutputs(account.getAddress(isSegwitAddress), AbstractHD.PathType.EXTERNAL_ROOT_PATH,
                                account.issuedExternalIndex(pathType) == 0 ? 0 : account.issuedExternalIndex(pathType) + 1, false);
                    }
                }
            }));
        }
        return actions;
    }
}
