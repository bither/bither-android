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

package net.bither.ui.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.bither.R;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.Qr;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-5-23.
 */
public class DialogFancyQrCode extends Dialog implements View.OnClickListener,
        DialogInterface.OnDismissListener, FancyQrCodeThread.FancyQrCodeListener {
    private Activity activity;
    private ImageView ivQr;
    private ProgressBar pb;
    private String content;
    private Bitmap qrCode;
    private int clickedView = 0;

    public DialogFancyQrCode(Activity context, String content) {
        this(context, content, AppSharedPreference.getInstance().hasUserAvatar());
    }

    public DialogFancyQrCode(Activity context, String content, boolean addAvatar) {
        this(context, content, addAvatar, false);
    }

    public DialogFancyQrCode(Activity context, String content, boolean addAvatar,
                             boolean blackAndWhite) {
        super(context, R.style.tipsDialog);
        this.activity = context;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getAttributes().dimAmount = 0.8f;
        setCanceledOnTouchOutside(true);
        this.content = content;
        setContentView(R.layout.dialog_fancy_qr_code);
        setOnDismissListener(this);
        initView();
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        if (blackAndWhite) {
            new FancyQrCodeThread(this.content, ivQr.getLayoutParams().width, Color.BLACK,
                    Color.WHITE, this, false).start();
        } else {
            Qr.QrCodeTheme theme = AppSharedPreference.getInstance().getFancyQrCodeTheme();
            new FancyQrCodeThread(this.content, ivQr.getLayoutParams().width, theme.getFgColor(),
                    theme.getBgColor(), this, addAvatar).start();
        }
    }

    private void initView() {
        ivQr = (ImageView) findViewById(R.id.iv_qrcode);
        pb = (ProgressBar) findViewById(R.id.pb);
        findViewById(R.id.ll_container).setOnClickListener(this);
        findViewById(R.id.ibtn_share).setOnClickListener(this);
        findViewById(R.id.ibtn_save).setOnClickListener(this);
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
            case R.id.ibtn_share:
                share();
                break;
            case R.id.ibtn_save:
                save();
                break;
            default:
                break;
        }
    }

    private void share() {
        if (qrCode != null) {
            new ShareThread().start();
        }
    }

    private void save() {
        if (qrCode != null) {
            new SaveThread().start();
        }
    }

    @Override
    public void generated(Bitmap bmp) {
        pb.setVisibility(View.GONE);
        qrCode = bmp;
        ivQr.setImageBitmap(bmp);
    }

    private class SaveThread extends Thread {
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            ImageFileUtil.saveImageToDcim(qrCode, 0, time);
            DropdownMessage.showDropdownMessage(activity, R.string.fancy_qr_code_save_success);
        }
    }

    private class ShareThread extends Thread {
        @Override
        public void run() {
            final Uri uri = FileUtil.saveShareImage(qrCode);
            if (uri == null) {
                DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                return;
            }
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType("image/jpg");
                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                    }
                }
            });
        }
    }
}
