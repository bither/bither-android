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

package net.bither.runnable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.listener.GetAvatarListener;
import net.bither.util.ImageManageUtil;
import net.bither.util.Qr;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

public class FancyQrCodeThread extends Thread {
    public static interface FancyQrCodeListener {
        public void generated(Bitmap bmp);
    }

    public static final float AvatarSizeRate = 0.24f;
    public static final int MarginSize = UIUtil.dip2pix(16);
    private FancyQrCodeListener listener;
    private String content;
    private int size;
    private int fgColor = Color.BLACK;
    private int bgColor = Color.WHITE;
    private boolean addAvatar;

    public FancyQrCodeThread(String content, int size, int fgColor, int bgColor,
                             FancyQrCodeListener listener) {
        this(content, size, fgColor, bgColor, listener, true);
    }

    public FancyQrCodeThread(String content, int size, int fgColor, int bgColor,
                             FancyQrCodeListener listener, boolean addAvatar) {
        this.content = content;
        this.listener = listener;
        this.size = size;
        this.fgColor = fgColor;
        this.bgColor = bgColor;
        this.addAvatar = addAvatar;
    }

    @Override
    public void run() {
        final Bitmap qrCode = Qr.bitmap(content, size, fgColor, bgColor, MarginSize);
        final int qrCodeSize = Math.min(qrCode.getWidth(), qrCode.getHeight());
        if (addAvatar && AppSharedPreference.getInstance().hasUserAvatar()) {
            ImageManageUtil.getAvatarForFancyQrCode(new GetAvatarListener() {
                @Override
                public void success(Bitmap bit) {
                    if (bit != null) {
                        Canvas c = new Canvas(qrCode);
                        int avatarSize = (int) (qrCodeSize * AvatarSizeRate);
                        int avaterOffset = (qrCodeSize - avatarSize) / 2;
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        c.drawBitmap(bit, null, new Rect(avaterOffset, avaterOffset,
                                avaterOffset + avatarSize, avaterOffset + avatarSize), paint);
                    }
                }

                @Override
                public void fileNoExist() {

                }
            });

        }

        if (listener != null) {
            ThreadUtil.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.generated(qrCode);
                }
            });
        }
    }
}
