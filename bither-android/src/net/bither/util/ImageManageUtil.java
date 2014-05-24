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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;

import net.bither.BitherApplication;

import java.io.File;

public class ImageManageUtil {
    public static int IMAGE_SIZE=612;

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

    public static Bitmap getBitmapNearestSize(String fileName, int size) {
        try {
            File file = new File(fileName);
            if (file == null || !file.exists()) {
                return null;
            } else if (file.length() == 0) {
                file.delete();
                return null;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, opts);
            int sampleSize = getSampleSize(
                    Math.min(opts.outHeight, opts.outWidth), size);
            opts.inSampleSize = sampleSize;
            opts.inJustDecodeBounds = false;
            opts.inPurgeable = true;
            opts.inInputShareable = false;
            opts.inPreferredConfig = Config.ARGB_8888;
            Bitmap bit = BitmapFactory.decodeFile(fileName, opts);
            return bit;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private static Bitmap getBitmapNearestSize(byte[] bytes, int size) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            int bmpSize = Math.min(opts.outHeight, opts.outWidth);
            opts.inSampleSize = getSampleSize(bmpSize, size);
            opts.inJustDecodeBounds = false;
            opts.inPurgeable = true;
            opts.inInputShareable = false;
            opts.inPreferredConfig = Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
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

            for (int i = 1; i <= sampleLessThanSize; i++) {
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
