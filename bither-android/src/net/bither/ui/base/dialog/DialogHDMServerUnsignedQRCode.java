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
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import net.bither.R;
import net.bither.util.ThreadUtil;

/**
 * Created by songchenwen on 15/1/10.
 */
public class DialogHDMServerUnsignedQRCode extends DialogSimpleQr implements View
        .OnClickListener, DialogInterface.OnDismissListener {
    public static interface DialogHDMServerUnsignedQRCodeListener {
        public void scanSignedHDMServerQRCode();
    }

    private DialogHDMServerUnsignedQRCodeListener listener;
    private View clickedView;
    private Button btn;

    public DialogHDMServerUnsignedQRCode(Context context, String content,
                                         DialogHDMServerUnsignedQRCodeListener listener) {
        super(context, content, R.string.hdm_keychain_add_unsigned_server_qr_code_title);
        this.listener = listener;
        FrameLayout fl = (FrameLayout) findViewById(R.id.fl_actions);
        btn = (Button) LayoutInflater.from(context).inflate(R.layout
                .dialog_hdm_server_unsigned_qr_code, fl, false);
        fl.addView(btn);
        btn.setOnClickListener(this);
        setOnDismissListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn) {
            clickedView = v;
            doDismiss();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void dismiss() {
        new DialogConfirmTask(getContext(), getContext().getString(R.string
                .hdm_keychain_add_unsigned_server_qr_code_dismiss_confirm), new Runnable() {
            @Override
            public void run() {
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        doDismiss();
                    }
                });
            }
        }).show();
    }

    private void doDismiss() {
        super.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedView == btn && listener != null) {
            listener.scanSignedHDMServerQRCode();
        }
    }
}
