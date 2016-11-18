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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 2016/11/3.
 */

public class DialogAddressQrCopy extends Dialog implements FancyQrCodeThread.FancyQrCodeListener,
        View.OnClickListener {
    private ImageView ivQr;
    private ProgressBar pb;
    private TextView tvAddress;
    private FrameLayout flAddress;
    private TextView tvTitle;
    private String address;
    private String title;

    public DialogAddressQrCopy(Context context, String address) {
        this(context, address, null);
    }

    public DialogAddressQrCopy(Context context, String address, int title) {
        this(context, address, context.getString(title));
    }

    public DialogAddressQrCopy(Context context, String address, String title) {
        super(context, R.style.tipsDialog);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getAttributes().dimAmount = 0.8f;
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.dialog_address_qr_copy);
        this.address = address;
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        flAddress = (FrameLayout) findViewById(R.id.fl_address);
        if (!Utils.isEmpty(title)) {
            tvTitle.setText(title);
        }
        this.title = title;
        ivQr = (ImageView) findViewById(R.id.iv_qrcode);
        pb = (ProgressBar) findViewById(R.id.pb);
        findViewById(R.id.ll_container).setOnClickListener(this);
        ivQr.setOnClickListener(this);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        ivQr.getLayoutParams().width = ivQr.getLayoutParams().height = size;
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams
                .MATCH_PARENT);
        new FancyQrCodeThread(address, ivQr.getLayoutParams().width, Color.BLACK, Color.WHITE,
                this, false).start();
        tvAddress.setText(WalletUtils.formatHash(address, 4, 20));
        flAddress.setOnClickListener(copyClick);
    }

    @Override
    public void generated(Bitmap bmp) {
        pb.setVisibility(View.GONE);
        ivQr.setImageBitmap(bmp);
    }

    private View.OnClickListener copyClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (address != null) {
                StringUtil.copyString(address);
                tvTitle.setText(R.string.copy_address_success);
                tvTitle.removeCallbacks(resetTitleRunnable);
                tvTitle.postDelayed(resetTitleRunnable, 800);
            }
        }
    };

    private Runnable resetTitleRunnable = new Runnable() {
        @Override
        public void run() {
            tvTitle.setText(title);
        }
    };

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
