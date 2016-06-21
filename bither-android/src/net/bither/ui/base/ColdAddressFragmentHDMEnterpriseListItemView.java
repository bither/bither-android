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

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.bither.R;
import net.bither.activity.cold.SignTxActivity;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;
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
 * Created by songchenwen on 15/1/7.
 */
public class ColdAddressFragmentHDMEnterpriseListItemView extends FrameLayout {
    private Activity activity;
    private EnterpriseHDMSeed seed;
    private ImageView ivType;
    private ImageButton ibtnXRandomLabel;
    private DialogProgress dp;

    public ColdAddressFragmentHDMEnterpriseListItemView(Activity context) {
        super(context);
        activity = context;
        View v = LayoutInflater.from(activity).inflate(R.layout
                .list_item_address_fragment_cold_hdm_enterprise, null);
        addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initView();
    }

    private void initView() {
        ivType = (ImageView) findViewById(R.id.iv_type);
        ibtnXRandomLabel = (ImageButton) findViewById(R.id.ibtn_xrandom_label);
        ibtnXRandomLabel.setOnLongClickListener(DialogXRandomInfo.InfoLongClick);
        ivType.setOnLongClickListener(typeClick);
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

    private OnClickListener seedOptionClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new DialogWithActions(v.getContext()) {

                @Override
                protected List<Action> getActions() {
                    ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions
                            .Action>();
                    actions.add(new Action(R.string.enterprise_hdm_seed_backup_qr_code, new
                            Runnable() {
                        @Override
                        public void run() {
                            final DialogPassword.PasswordGetter passwordGetter = new
                                    DialogPassword.PasswordGetter(getContext());
                            new Thread() {
                                @Override
                                public void run() {
                                    SecureCharSequence password = passwordGetter.getPassword();
                                    if (password == null) {
                                        return;
                                    }
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dp.show();
                                        }
                                    });
                                    final String content = QRCodeUtil.Enterprise_HDM_QR_CODE_FLAG
                                            + seed.getEncryptedMnemonicSeed();
                                    password.wipe();
                                    passwordGetter.wipe();
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dp.dismiss();
                                            if (content != null) {
                                                new DialogSimpleQr(getContext(), content, R
                                                        .string
                                                        .enterprise_hdm_seed_backup_qr_code_promote).show();
                                            }
                                        }
                                    });
                                }
                            }.start();
                        }
                    }));
                    actions.add(new Action(R.string.enterprise_hdm_seed_backup_phrase, new
                            Runnable() {
                        @Override
                        public void run() {
                            final DialogPassword.PasswordGetter passwordGetter = new
                                    DialogPassword.PasswordGetter(getContext());
                            new Thread() {
                                @Override
                                public void run() {
                                    SecureCharSequence password = passwordGetter.getPassword();
                                    if (password == null) {
                                        return;
                                    }
                                    ThreadUtil.runOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dp.show();
                                        }
                                    });
                                    try {
                                        final List<String> words = seed.getSeedWords(password);
                                        password.wipe();
                                        passwordGetter.wipe();
                                        ThreadUtil.runOnMainThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dp.dismiss();
                                                if (words != null) {
                                                    new DialogHDMSeedWordList(getContext(),
                                                            words).show();
                                                }
                                            }
                                        });
                                    } catch (MnemonicException.MnemonicLengthException e) {
                                        password.wipe();
                                        passwordGetter.wipe();
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
                    }));
                    return actions;
                }
            }.show();
        }
    };

    private OnClickListener qrCodeOptionClick = new DialogWithActions
            .DialogWithActionsClickListener() {

        @Override
        protected List<DialogWithActions.Action> getActions() {
            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            actions.add(new DialogWithActions.Action(R.string.enterprise_hdm_seed_qr_code, new
                    Runnable() {

                @Override
                public void run() {
                    new DialogPassword(getContext(), new IDialogPasswordListener() {
                        @Override
                        public void onPasswordEntered(SecureCharSequence password) {
                            showPublicKeyQrCode(password);
                        }
                    }).show();
                }
            }));
            actions.add(new DialogWithActions.Action(R.string.enterprise_hdm_seed_cosign, new
                    Runnable() {

                @Override
                public void run() {
                    Intent intent = new Intent(getContext(), SignTxActivity.class);
                    getContext().startActivity(intent);
                }
            }));
            return actions;
        }

        private void showPublicKeyQrCode(final SecureCharSequence password) {
            if (!dp.isShowing()) {
                dp.show();
            }
            new Thread() {
                @Override
                public void run() {
                    try {
                        final String pub = EnterpriseHDMSeed.XPubPrefix +
                                Utils.bytesToHexString(
                                        seed.getExternalRootPubExtended(password)).toUpperCase();
                        password.wipe();
                        post(new Runnable() {
                            @Override
                            public void run() {
                                dp.dismiss();
                                new DialogSimpleQr(getContext(), pub, R.string
                                        .hdm_cold_pub_key_qr_code_name).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };


    public void setSeed(EnterpriseHDMSeed seed) {
        this.seed = seed;
        if (seed.isFromXRandom()) {
            ibtnXRandomLabel.setVisibility(View.VISIBLE);
        } else {
            ibtnXRandomLabel.setVisibility(View.INVISIBLE);
        }
    }
}
