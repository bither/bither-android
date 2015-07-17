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

package net.bither.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.HDAccountCold;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.ui.base.dialog.DialogHDMSeedWordList;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogSimpleQr;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.dialog.DialogXRandomInfo;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/6/25.
 */
public class ColdAddressFragmentHDAccountColdListItemView extends FrameLayout {

    private HDAccountCold hdAccountCold;

    private ImageButton ibtnXRandomLabel;
    private DialogProgress dp;

    public ColdAddressFragmentHDAccountColdListItemView(Context context) {
        super(context);
        init();
    }

    public ColdAddressFragmentHDAccountColdListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColdAddressFragmentHDAccountColdListItemView(Context context, AttributeSet attrs, int
            defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        hdAccountCold = AddressManager.getInstance().getHDAccountCold();
        View v = LayoutInflater.from(getContext()).inflate(R.layout
                .list_item_address_fragment_hd_account_cold, null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ibtnXRandomLabel = (ImageButton) findViewById(R.id.ibtn_xrandom_label);
        ibtnXRandomLabel.setOnLongClickListener(DialogXRandomInfo.InfoLongClick);
        if (hdAccountCold.isFromXRandom()) {
            ibtnXRandomLabel.setVisibility(View.VISIBLE);
        } else {
            ibtnXRandomLabel.setVisibility(View.GONE);
        }
        findViewById(R.id.iv_type).setOnLongClickListener(typeClick);
        findViewById(R.id.ibtn_seed_option).setOnClickListener(seedOptionClick);
        findViewById(R.id.ibtn_qr_code_option).setOnClickListener(qrCodeOptionClick);
        dp = new DialogProgress(getContext(), R.string.please_wait);
        dp.setCancelable(false);
    }

    private OnLongClickListener typeClick = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            seedOptionClick.onClick(v);
            return true;
        }
    };


    private OnClickListener seedOptionClick = new DialogWithActions
            .DialogWithActionsClickListener() {
        @Override
        protected List<DialogWithActions.Action> getActions() {
            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            actions.add(new DialogWithActions.Action(R.string.add_hd_account_seed_qr_code, new
                    Runnable() {
                @Override
                public void run() {
                    new DialogPassword(getContext(), new IDialogPasswordListener() {
                        @Override
                        public void onPasswordEntered(final SecureCharSequence password) {
                            password.wipe();
                            String content = hdAccountCold.getQRCodeFullEncryptPrivKey();
                            new DialogSimpleQr(getContext(), content, R.string
                                    .add_hd_account_seed_qr_code).show();
                        }
                    }).show();
                }
            }));
            actions.add(new DialogWithActions.Action(R.string.add_hd_account_seed_qr_phrase, new
                    Runnable() {
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
                                        words.addAll(hdAccountCold.getSeedWords(password));
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
                                                new DialogHDMSeedWordList(getContext(), words)
                                                        .show();
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
    };

    private OnClickListener qrCodeOptionClick = new DialogWithActions
            .DialogWithActionsClickListener() {

        @Override
        protected List<DialogWithActions.Action> getActions() {
            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            actions.add(new DialogWithActions.Action(R.string.add_cold_hd_account_monitor_qr, new
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
                                        final String content = hdAccountCold
                                                .accountPubExtendedString(password);
                                        ThreadUtil.runOnMainThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dp.dismiss();
                                                new DialogSimpleQr(getContext(), content, R
                                                        .string.add_cold_hd_account_monitor_qr)
                                                        .show();
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
            return actions;
        }
    };

}
