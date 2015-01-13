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

package net.bither.qrcode;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

import javax.annotation.Nonnull;

public class Qr {
    public static enum QrCodeTheme {
        YELLOW("#ff835229", "#ffe7e1c7", R.string.fancy_qr_code_theme_name_yellow),
        GREEN("#ff486804", "#fffcfdf9", R.string.fancy_qr_code_theme_name_green),
        BLUE("#ff025c7f", "#ffeff4f7", R.string.fancy_qr_code_theme_name_blue),
        RED("#ff922c15", "#fffefaf9", R.string.fancy_qr_code_theme_name_red),
        PURPLE("#ff8f127f", "#ffe2f5ee", R.string.fancy_qr_code_theme_name_purple),
        BLACK("#ff000000", "#ffffffff", R.string.fancy_qr_code_theme_name_black);

        private int fgColor;
        private int bgColor;
        private int title;

        QrCodeTheme(String fg, String bg, int title) {
            fgColor = Color.parseColor(fg);
            bgColor = Color.parseColor(bg);
            this.title = title;
        }

        public int getFgColor() {
            return fgColor;
        }

        public int getBgColor() {
            return bgColor;
        }

        public String getTitle() {
            return BitherApplication.mContext.getString(title);
        }
    }

    private final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    private static final Logger log = LoggerFactory.getLogger(Qr.class);

    public static Bitmap bitmap(@Nonnull final String content, final int size) {
        return bitmap(content, size, Color.BLACK, Color.TRANSPARENT);
    }

    public static Bitmap bitmap(@Nonnull final String content, final int size, int fgColor,
                                int bgColor) {
        return bitmap(content, size, fgColor, bgColor, -1);
    }

    public static Bitmap bitmap(@Nonnull final String content, final int size, int fgColor,
                                int bgColor, int margin) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            final BitMatrix result = QR_CODE_WRITER.encode(content, BarcodeFormat.QR_CODE, size,
                    size, hints);
            int[] drawBeginLocation = new int[]{0, 0};
            int dataWidth = result.getWidth();
            int dataHeight = result.getHeight();
            int outWidth = result.getWidth();
            int outHeight = result.getHeight();
            if (margin >= 0) {
                int[] drawRectangle = result.getEnclosingRectangle();
                int left = drawRectangle[0];
                int top = drawRectangle[1];
                int right = outWidth - drawRectangle[2] - left;
                int bottom = outHeight - drawRectangle[3] - top;
                int maxOriMargin = Math.max(Math.max(top, bottom), Math.max(left, right));
                if (margin > maxOriMargin) {
                    dataWidth = drawRectangle[2];
                    dataHeight = drawRectangle[3];
                    drawBeginLocation[0] = drawRectangle[0];
                    drawBeginLocation[1] = drawRectangle[1];
                    outWidth = dataWidth + margin * 2;
                    outHeight = dataHeight + margin * 2;
                }
            }
            final int[] pixels = new int[outWidth * outHeight];

            int startX = (outWidth - dataWidth) / 2;
            int startY = (outHeight - dataHeight) / 2;

            for (int y = 0;
                 y < outHeight;
                 y++) {
                final int offset = y * outWidth;
                for (int x = 0;
                     x < outWidth;
                     x++) {
                    if (x >= startX && x < dataWidth + startX && y >= startY && y < dataHeight +
                            startY) {
                        pixels[offset + x] = result.get(x - startX + drawBeginLocation[0],
                                y - startY + drawBeginLocation[1]) ? fgColor : bgColor;
                    } else {
                        pixels[offset + x] = bgColor;
                    }
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight);
            return bitmap;
        } catch (final WriterException x) {
            log.info("problem creating qr code", x);
            return null;
        }
    }


    public static void printQrContentSize() {
        for (int versionNum = 1;
             versionNum <= 40;
             versionNum++) {
            Version version = Version.getVersionForNumber(versionNum);
            // numBytes = 196
            int numBytes = version.getTotalCodewords();
            // getNumECBytes = 130
            Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ErrorCorrectionLevel.L);
            int numEcBytes = ecBlocks.getTotalECCodewords();
            // getNumDataBytes = 196 - 130 = 66
            int numDataBytes = numBytes - numEcBytes;
            int numInputBytes = numDataBytes * 8 - 7;
            int length = (numInputBytes - 10) / 11 * 2;
            LogUtil.d("Qr", "Version: " + versionNum + " numData bytes: " + numDataBytes + "  " +
                    "input: " + numInputBytes + "  string length: " + length);
        }
    }
}
