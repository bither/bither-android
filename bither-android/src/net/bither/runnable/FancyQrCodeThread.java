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
import android.graphics.Paint;
import android.graphics.Rect;

import net.bither.util.ImageManageUtil;
import net.bither.util.Qr;
import net.bither.util.ThreadUtil;

/**
 * Created by songchenwen on 14-5-24.
 */
public class FancyQrCodeThread extends Thread {
    public static interface FancyQrCodeListener {
        public void generated(Bitmap bmp);
    }

    public static final float AvatarSizeRate = 0.16f;
    private FancyQrCodeListener listener;
    private String content;
    private int size;
    private int fgColor;
    private int bgColor;

    public FancyQrCodeThread(String content, int size, int fgColor, int bgColor,
                             FancyQrCodeListener listener) {
        this.content = content;
        this.listener = listener;
        this.size = size;
        this.fgColor = fgColor;
        this.bgColor = bgColor;
    }

    @Override
    public void run() {
        final Bitmap qrCode = Qr.bitmap(content, size, fgColor, bgColor);
        Bitmap avatar = ImageManageUtil.getAvatarForFancyQrCode();
        if (avatar != null) {
            Canvas c = new Canvas(qrCode);
            int avatarSize = (int) (size * AvatarSizeRate);
            int avaterOffset = (size - avatarSize) / 2;
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            c.drawBitmap(avatar, null, new Rect(avaterOffset, avaterOffset,
                    avaterOffset + avatarSize, avaterOffset + avatarSize), paint);
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
