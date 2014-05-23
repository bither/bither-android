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

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.bither.R;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-5-23.
 */
public class DialogFancyQrCode extends CenterDialog {
    private static final int Margin = UIUtil.dip2pix(20);
    private QrCodeImageView ivQr;
    private ImageView ivAvatar;
    private FrameLayout flContent;
    private String content;

    private View.OnClickListener dismissClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };

    public DialogFancyQrCode(Context context, String content) {
        super(context);
        this.content = content;
        setContentView(R.layout.dialog_fancy_qr_code);
        initView();
    }

    private void initView() {
        ivQr = (QrCodeImageView) findViewById(R.id.iv_qrcode);
        ivAvatar = (ImageView) findViewById(R.id.iv_avatar);
        flContent = (FrameLayout) findViewById(R.id.fl_content);
        container.setOnClickListener(dismissClick);
        int size = Math.min(UIUtil.getScreenWidth(), UIUtil.getScreenHeight()) - Margin * 2 - container.getPaddingLeft() - container.getPaddingRight();
        ivQr.getLayoutParams().width = ivQr.getLayoutParams().height = size;
        ivQr.setContent(content, getContext().getResources().getColor(R.color.fancy_qr_code_fg), getContext().getResources().getColor(R.color.fancy_qr_code_bg));
    }
}
