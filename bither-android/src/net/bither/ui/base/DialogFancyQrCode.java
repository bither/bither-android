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
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.bither.R;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.ImageManageUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-5-23.
 */
public class DialogFancyQrCode extends Dialog implements View.OnClickListener, DialogInterface.OnDismissListener {
    private static final float AvatarSizeRate = 0.2f;
    private Activity activity;
    private QrCodeImageView ivQr;
    private ImageView ivAvatar;
    private FrameLayout flContent;
    private String content;
    private int clickedView = 0;

    public DialogFancyQrCode(Activity context, String content) {
        super(context, R.style.tipsDialog);
        this.activity = context;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getAttributes().dimAmount = 0.75f;
        setCanceledOnTouchOutside(true);
        this.content = content;
        setContentView(R.layout.dialog_fancy_qr_code);
        setOnDismissListener(this);
        initView();
    }

    private void initView() {
        ivQr = (QrCodeImageView) findViewById(R.id.iv_qrcode);
        ivAvatar = (ImageView) findViewById(R.id.iv_avatar);
        flContent = (FrameLayout) findViewById(R.id.fl_content);
        findViewById(R.id.ll_container).setOnClickListener(this);
        findViewById(R.id.btn_share).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
        ivQr.setOnClickListener(this);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight());
        ivQr.getLayoutParams().width = ivQr.getLayoutParams().height = size;
        ivAvatar.getLayoutParams().width = ivAvatar.getLayoutParams().height = (int) (size * AvatarSizeRate);
        ivQr.setContent(content, getContext().getResources().getColor(R.color.fancy_qr_code_fg), getContext().getResources().getColor(R.color.fancy_qr_code_bg));
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
            case R.id.btn_share:
                share();
                break;
            case R.id.btn_save:
                save();
                break;
            default:
                break;
        }
    }

    private void save() {
        new SaveThread().start();
    }

    private void share() {
        new ShareThread().start();
    }

    private class SaveThread extends Thread {
        @Override
        public void run() {
            Bitmap content = ImageManageUtil.getBitmapFromView(flContent);
            long time = System.currentTimeMillis();
            ImageFileUtil.saveImageToDcim(content, 0, time);
            DropdownMessage.showDropdownMessage(activity, R.string.fancy_qr_code_save_success);
        }
    }

    private class ShareThread extends Thread {
        @Override
        public void run() {
            Bitmap content = ImageManageUtil.getBitmapFromView(flContent);
            final Uri uri = FileUtil.saveShareImage(content);
            if (uri == null) {
                DropdownMessage.showDropdownMessage(activity, R.string.market_share_failed);
                return;
            }
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(
                            android.content.Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType("image/jpg");
                    getContext().startActivity(intent);

                }
            });
        }
    }
}
