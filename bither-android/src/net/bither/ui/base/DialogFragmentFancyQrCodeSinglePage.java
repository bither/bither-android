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


import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import net.bither.R;
import net.bither.runnable.FancyQrCodeThread;
import net.bither.util.ImageManageUtil;
import net.bither.util.Qr;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

/**
 * Created by songchenwen on 14-6-6.
 */
public class DialogFragmentFancyQrCodeSinglePage extends Fragment implements FancyQrCodeThread
        .FancyQrCodeListener {
    public static final String ContentTag = "Content";
    public static final String ThemeTag = "Theme";

    private static final int QrCodeSize = Math.min(UIUtil.getScreenHeight(),
            UIUtil.getScreenWidth());

    private String content;
    private Qr.QrCodeTheme theme;
    private FrameLayout flQrContainer;
    private ImageView ivAvatar;
    private ImageView ivQr;
    private ProgressBar pb;
    private Bitmap avatar;
    private Bitmap qrCode;

    public static DialogFragmentFancyQrCodeSinglePage newInstance(String content,
                                                                  Qr.QrCodeTheme theme) {
        DialogFragmentFancyQrCodeSinglePage page = new DialogFragmentFancyQrCodeSinglePage();
        Bundle bundle = new Bundle();
        bundle.putString(ContentTag, content);
        if (theme != null) {
            bundle.putInt(ThemeTag, theme.ordinal());
        } else {
            bundle.putInt(ThemeTag, -1);
        }
        page.setArguments(bundle);
        return page;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        content = bundle.getString(ContentTag);
        int themeOrdinal = bundle.getInt(ThemeTag, -1);
        if (themeOrdinal >= 0 && themeOrdinal < Qr.QrCodeTheme.values().length) {
            theme = Qr.QrCodeTheme.values()[themeOrdinal];
        } else {
            theme = null;
        }
        if (theme != null) {
            new FancyQrCodeThread(content, QrCodeSize, theme.getFgColor(), theme.getBgColor(),
                    this, false).start();
        } else {
            new FancyQrCodeThread(content, QrCodeSize, Color.BLACK, Color.WHITE, this,
                    false).start();
        }
        if (theme != null) {
            new GetAvatarThread().start();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_qr_code_single_page, null);
        flQrContainer = (FrameLayout) v.findViewById(R.id.fl_qr_container);
        ivQr = (ImageView) v.findViewById(R.id.iv_qrcode);
        ivAvatar = (ImageView) v.findViewById(R.id.iv_avatar);
        pb = (ProgressBar) v.findViewById(R.id.pb);
        ivQr.getLayoutParams().height = ivQr.getLayoutParams().width = QrCodeSize;
        ivAvatar.getLayoutParams().height = ivAvatar.getLayoutParams().height = (int) (QrCodeSize
                * FancyQrCodeThread.AvatarSizeRate);
        configureImages();
        return v;
    }

    public Bitmap getQrCode() {
        if (flQrContainer.getVisibility() == View.VISIBLE) {
            if (ivAvatar.getVisibility() == View.VISIBLE) {
                return ImageManageUtil.getBitmapFromView(flQrContainer);
            } else {
                return qrCode;
            }
        } else {
            return null;
        }
    }

    @Override
    public void generated(Bitmap bmp) {
        qrCode = bmp;
        configureImages();
    }

    private void configureImages() {
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (qrCode != null && ivQr != null) {
                    ivQr.setImageBitmap(qrCode);
                }
                if (theme != null && avatar != null && ivAvatar != null) {
                    ivAvatar.setImageBitmap(avatar);
                }
                configureProgress();
            }
        });
    }

    private void configureProgress() {
        if (qrCode != null && (theme == null || avatar != null) && flQrContainer != null) {
            flQrContainer.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
        } else {
            flQrContainer.setVisibility(View.INVISIBLE);
            pb.setVisibility(View.VISIBLE);
        }
    }

    private class GetAvatarThread extends Thread {
        @Override
        public void run() {
            avatar = ImageManageUtil.getAvatarForFancyQrCode();
            if (ivAvatar != null) {
                configureImages();
            }
        }
    }
}
