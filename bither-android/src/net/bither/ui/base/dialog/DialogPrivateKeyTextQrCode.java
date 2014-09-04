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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.bither.R;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.UIUtil;

public class DialogPrivateKeyTextQrCode extends Dialog implements View.OnClickListener,
        DialogInterface.OnDismissListener, FancyQrCodeThread.FancyQrCodeListener {
    private Activity activity;
    private ImageView ivQr;
    private ProgressBar pb;
    private String content;
    private Bitmap qrCode;
    private int clickedView = 0;

    public DialogPrivateKeyTextQrCode(Activity context, String privateText) {
        super(context, R.style.tipsDialog);
        this.activity = context;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getAttributes().dimAmount = 0.8f;
        setCanceledOnTouchOutside(true);
        this.content = privateText;
        setContentView(R.layout.dialog_private_key_qr_code);
        setOnDismissListener(this);
        initView();
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        new FancyQrCodeThread(this.content, ivQr.getLayoutParams().width, Color.BLACK,
                Color.WHITE, this, false).start();
    }

    private void initView() {
        ivQr = (ImageView) findViewById(R.id.iv_qrcode);
        pb = (ProgressBar) findViewById(R.id.pb);
        findViewById(R.id.ll_container).setOnClickListener(this);
        findViewById(R.id.ll_back_up).setVisibility(View.GONE);
        ivQr.setOnClickListener(this);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        ivQr.getLayoutParams().width = ivQr.getLayoutParams().height = size;
    }

    @Override
    public void show() {
        clickedView = 0;
        super.show();
    }

    @Override
    public void onClick(View v) {
        clickedView = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (clickedView) {
            default:
                break;
        }
    }

   
    @Override
    public void generated(Bitmap bmp) {
        pb.setVisibility(View.GONE);
        qrCode = bmp;
        ivQr.setImageBitmap(bmp);
    }


}
