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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;
import net.bither.activity.cold.BitpieColdSignMessageActivity;
import net.bither.bitherj.utils.Utils;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 15/1/8.
 */
public class DialogSimpleQr extends Dialog implements FancyQrCodeThread.FancyQrCodeListener,
        View.OnClickListener {
    private ImageView ivQr;
    private ProgressBar pb;
    private TextView tvSubtitle;
    private Button btnSign;
    private Context context;
    private TextView tvScanTip;

    public DialogSimpleQr(Context context, String content) {
        this(context, content, null);
    }

    public DialogSimpleQr(Context context, String content, int title) {
        this(context, content, context.getString(title));
    }

    public DialogSimpleQr(Context context, String content, String title) {
        this(context, content, title, null);
    }

    public DialogSimpleQr(Context context, String content, int title, int subtitle) {
        this(context, content, context.getString(title), context.getString(subtitle));
    }

    public DialogSimpleQr(Context context, String content, int title, String subtitle) {
        this(context, content, context.getString(title), subtitle);
    }

    public DialogSimpleQr(Context context, String content, String title, int subtitle) {
        this(context, content, title, context.getString(subtitle));
    }

    public DialogSimpleQr(Context context, String content, String title, String subtitle) {
        super(context, R.style.tipsDialog);
        this.context = context;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getAttributes().dimAmount = 0.8f;
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.dialog_simple_qr);
        if (!Utils.isEmpty(title)) {
            TextView tvTitle = (TextView) findViewById(R.id.tv_title);
            tvTitle.setText(title);
            btnSign = findViewById(R.id.btn_sign);
            tvScanTip = findViewById(R.id.tv_scan_tip);
            if (title.equals(context.getString(R.string.add_bitpie_cold_hd_account_monitor_qr))) {
                tvScanTip.setVisibility(View.VISIBLE);
                btnSign.setVisibility(View.VISIBLE);
                btnSign.setOnClickListener(toBitpieColdSignMessageActivityClickListener);
            }
        }
        ivQr = (ImageView) findViewById(R.id.iv_qrcode);
        pb = (ProgressBar) findViewById(R.id.pb);
        tvSubtitle = (TextView) findViewById(R.id.tv_subtitle);
        if (!Utils.isEmpty(subtitle)) {
            tvSubtitle.setText(subtitle);
            tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }
        findViewById(R.id.ll_container).setOnClickListener(this);
        ivQr.setOnClickListener(this);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        ivQr.getLayoutParams().width = ivQr.getLayoutParams().height = size;
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        new FancyQrCodeThread(content, ivQr.getLayoutParams().width, Color.BLACK, Color.WHITE,
                this, false).start();
    }

    private View.OnClickListener toBitpieColdSignMessageActivityClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(context, BitpieColdSignMessageActivity.class);
            context.startActivity(intent);
            dismiss();
        }
    };

    @Override
    public void generated(Bitmap bmp) {
        pb.setVisibility(View.GONE);
        ivQr.setImageBitmap(bmp);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
