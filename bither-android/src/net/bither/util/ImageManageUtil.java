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

package net.bither.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;

import net.bither.BitherApplication;
import net.bither.R;

public class ImageManageUtil {

    public static Bitmap getBitmapFromView(View v, int width, int height) {
        v.measure(width, height);
        v.layout(0, 0, width, height);
        Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return bmp;
    }

    public static Bitmap getBitmapFromView(View v) {
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
                Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return bmp;
    }

    public static final int getStatusBarHeight(Window window) {
        Rect frame = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(frame);
        return frame.top;
    }

    public static Bitmap getAvatarForFancyQrCode() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Resources res = BitherApplication.mContext.getResources();
        Bitmap shape = BitmapFactory.decodeResource(res, R.drawable.avatar_for_fancy_qr_code_shape);
        Bitmap result = Bitmap.createBitmap(shape.getWidth(), shape.getHeight(), shape.getConfig());
        Canvas c = new Canvas(result);
        c.drawBitmap(shape, 0, 0, paint);
        shape = null;
        Paint avatarPaint = new Paint();
        avatarPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        avatarPaint.setAntiAlias(true);
        // TODO get avatar
        Bitmap avatar = BitmapFactory.decodeResource(res, R.drawable.avatar_test);
        c.drawBitmap(avatar, null, new Rect(0, 0, result.getWidth(), result.getHeight()),
                avatarPaint);
        avatar = null;
        Bitmap overlay = BitmapFactory.decodeResource(res,
                R.drawable.avatar_for_fancy_qr_code_overlay);
        c.drawBitmap(overlay, null, new Rect(0, 0, result.getWidth(), result.getHeight()), paint);
        overlay = null;
        return result;
    }

}
