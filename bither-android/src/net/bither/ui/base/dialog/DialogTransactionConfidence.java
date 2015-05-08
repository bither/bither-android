/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.Tx;
import net.bither.util.UIUtil;

import java.util.Arrays;
import java.util.List;

public class DialogTransactionConfidence extends DialogWithArrow implements DialogInterface
        .OnShowListener {
    private ProgressBar pb;
    private ImageView ivHot;
    private ImageView ivCold;
    private ImageView ivServer;

    private Tx tx;
    private Address address;

    private boolean showSigningInfo;

    public DialogTransactionConfidence(Context context, Tx tx, Address address) {
        super(context);
        this.tx = tx;
        this.address = address;
        setContentView(R.layout.dialog_transaction_confidence);
        TextView tv = (TextView) findViewById(R.id.tv_confirmation);
        long confidence = tx.getConfirmationCount();
        if (confidence <= 100) {
            tv.setText(Long.toString(confidence));
        } else {
            tv.setText("100+");
        }
        View llIconContainer = findViewById(R.id.ll_icon_container);
        pb = (ProgressBar) findViewById(R.id.pb);
        ivHot = (ImageView) findViewById(R.id.iv_hot);
        ivCold = (ImageView) findViewById(R.id.iv_cold);
        ivServer = (ImageView) findViewById(R.id.iv_server);
        showSigningInfo = address.isHDM() && tx.deltaAmountFrom(address) < 0;
        if (showSigningInfo) {
            llIconContainer.setVisibility(View.VISIBLE);
            pb.setVisibility(View.VISIBLE);
            ivHot.setVisibility(View.GONE);
            ivCold.setVisibility(View.GONE);
            ivServer.setVisibility(View.GONE);
        } else {
            pb.setVisibility(View.GONE);
            llIconContainer.setVisibility(View.GONE);
        }
        setOnShowListener(this);
    }

    @Override
    public void show() {
        ivHot.setVisibility(View.GONE);
        ivCold.setVisibility(View.GONE);
        ivServer.setVisibility(View.GONE);
        if (showSigningInfo) {
            pb.setVisibility(View.VISIBLE);
        } else {
            pb.setVisibility(View.GONE);
        }
        super.show();
    }

    @Override
    public int getSuggestHeight() {
        if (showSigningInfo) {
            return UIUtil.dip2pix(68);
        } else {
            return UIUtil.dip2pix(40);
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (showSigningInfo) {
            new Thread() {
                @Override
                public void run() {
                    HDMAddress hdm = (HDMAddress) address;
                    List<byte[]> signingPubs = tx.getIns().get(0).getP2SHPubKeys();
                    boolean isHot = false;
                    boolean isCold = false;
                    boolean isServer = false;
                    for (byte[] pub : signingPubs) {
                        if (!isHot && Arrays.equals(pub, hdm.getPubHot())) {
                            isHot = true;
                            continue;
                        }
                        if (!isCold && Arrays.equals(pub, hdm.getPubCold())) {
                            isCold = true;
                            continue;
                        }
                        if (!isServer && Arrays.equals(pub, hdm.getPubRemote())) {
                            isServer = true;
                            continue;
                        }
                    }
                    final boolean _isHot = isHot;
                    final boolean _isCold = isCold;
                    final boolean _isServer = isServer;
                    pb.post(new Runnable() {
                        @Override
                        public void run() {
                            if (_isHot) {
                                ivHot.setVisibility(View.VISIBLE);
                            } else {
                                ivHot.setVisibility(View.GONE);
                            }
                            if (_isCold) {
                                ivCold.setVisibility(View.VISIBLE);
                            } else {
                                ivCold.setVisibility(View.GONE);
                            }
                            if (_isServer) {
                                ivServer.setVisibility(View.VISIBLE);
                            } else {
                                ivServer.setVisibility(View.GONE);
                            }
                            pb.setVisibility(View.GONE);
                        }
                    });
                }
            }.start();
        }
    }
}
