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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.api.DownloadFile;
import net.bither.http.BitherUrl;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.listener.GetAvatarListener;

import java.io.File;

public class ImageManageUtil {
    public static int IMAGE_SMALL_SIZE = 150;
    public static int IMAGE_SIZE = 612;

    public static int getScreenWidth() {
        return BitherApplication.mContext.getResources().getDisplayMetrics().widthPixels;
    }

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

    public static Bitmap getMatrixBitmap(Bitmap bm, int w, int h,
                                         boolean needRecycleSource) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        boolean isCompress = (width > w && height > h) && (w != 0 && h != 0)
                && (w != width || h != height);
        if (isCompress) {
            float scaleWidth = ((float) w) / width;
            float scaleHeight = ((float) h) / height;
            float scale = Math.max(scaleWidth, scaleHeight);
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
                    matrix, true);
            if (needRecycleSource && bm != null && bm != bitmap) {
                bm.recycle();
            }
            return bitmap;
        } else {
            return bm;
        }
    }

    public static void getAvatarForFancyQrCode(GetAvatarListener getAvatarListener) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Resources res = BitherApplication.mContext.getResources();
        Bitmap shape = BitmapFactory.decodeResource(res, R.drawable.avatar_for_fancy_qr_code_shape);
        Bitmap result = Bitmap.createBitmap(shape.getWidth(), shape.getHeight(), shape.getConfig());
        Canvas c = new Canvas(result);
        c.drawBitmap(shape, 0, 0, paint);
        Paint avatarPaint = new Paint();
        avatarPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        avatarPaint.setAntiAlias(true);
        Bitmap avatarBit = getAvatarBit();
        if (avatarBit == null) {
            if (getAvatarListener != null) {
                getAvatarListener.fileNoExist();
            }
            return;
        }
        c.drawBitmap(avatarBit, null, new Rect(0, 0, result.getWidth(), result.getHeight()),
                avatarPaint);
        Bitmap overlay = BitmapFactory.decodeResource(res,
                R.drawable.avatar_for_fancy_qr_code_overlay);
        c.drawBitmap(overlay, null, new Rect(0, 0, result.getWidth(), result.getHeight()), paint);
        if (result != null) {
            if (getAvatarListener != null) {
                getAvatarListener.success(result);
            }
        }
    }

    private static Bitmap getAvatarBit() {
        String avatar = AppSharedPreference.getInstance().getUserAvatar();
        Bitmap avatarBit = null;
        if (!StringUtil.isEmpty(avatar)) {
            File file = ImageFileUtil.getSmallAvatarFile(avatar);
            avatarBit = ImageManageUtil.getBitmapNearestSize(file,
                    ImageManageUtil.IMAGE_SMALL_SIZE);
        }
        return avatarBit;
    }


    public static Bitmap getBitmapNearestSize(File file, int size) {
        try {
            if (file == null || !file.exists()) {
                return null;
            } else if (file.length() == 0) {
                file.delete();
                return null;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            int sampleSize = getSampleSize(
                    Math.min(opts.outHeight, opts.outWidth), size);
            opts.inSampleSize = sampleSize;
            opts.inJustDecodeBounds = false;
            opts.inPurgeable = true;
            opts.inInputShareable = false;
            opts.inPreferredConfig = Config.ARGB_8888;
            Bitmap bit = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            return bit;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getSampleSize(int fileSize, int targetSize) {
        int sampleSize = 1;
        if (fileSize > targetSize * 2) {
            int sampleLessThanSize = 0;
            do {
                sampleLessThanSize++;
            } while (fileSize / sampleLessThanSize > targetSize);

            for (int i = 1;
                 i <= sampleLessThanSize;
                 i++) {
                if (Math.abs(fileSize / i - targetSize) <= Math.abs(fileSize
                        / sampleSize - targetSize)) {
                    sampleSize = i;
                }
            }
        } else {
            if (fileSize <= targetSize) {
                sampleSize = 1;
            } else {
                sampleSize = 2;
            }
        }
        return sampleSize;
    }
}
