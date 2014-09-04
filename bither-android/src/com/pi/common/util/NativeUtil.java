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

package com.pi.common.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;

import net.bither.bitherj.utils.LogUtil;
import net.bither.util.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class NativeUtil {
    private static int DEFAULT_QUALITY=95;
    public static void compressBitmap(Bitmap bit, String fileName,
                                      boolean optimize){
        compressBitmap(bit,DEFAULT_QUALITY,fileName,optimize);

    }
    public static void compressBitmap(Bitmap bit, int quality, String fileName,
                                      boolean optimize) {
        LogUtil.d("native", "compress of native");
        if (bit.getConfig() != Config.ARGB_8888) {
            Bitmap result = null;

            result = Bitmap.createBitmap(bit.getWidth(), bit.getHeight(),
                    Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            Rect rect = new Rect(0, 0, bit.getWidth(), bit.getHeight());
            canvas.drawBitmap(bit, null, rect, null);
            saveBitmap(result, quality, fileName, optimize);
            result.recycle();
        } else {
            saveBitmap(bit, quality, fileName, optimize);
        }

    }

    private static void saveBitmap(Bitmap bit, int quality, String fileName,
                                   boolean optimize) {
        String compressResult = compressBitmap(bit, bit.getWidth(),
                bit.getHeight(), quality, fileName.getBytes(), optimize);
        LogUtil.d("native", "result:" + compressResult);
        if (!StringUtil.isInteger(compressResult)
                || Integer.valueOf(compressResult) != 1) {
            try {
                File bitmapFile = new File(fileName);
                bit.compress(CompressFormat.JPEG, 70, new FileOutputStream(
                        bitmapFile));
                LogUtil.d("native", "compress of bitmap");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            LogUtil.d("native", "compress of native");
        }
    }

    private static native String compressBitmap(Bitmap bit, int w, int h,
                                                int quality, byte[] fileNameBytes, boolean optimize);

    public static native String stringFromJNI(String str);


    static {
        System.loadLibrary("jpegpi");
        System.loadLibrary("piutil");
        System.loadLibrary("pijni");

    }

}
