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
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

public class Qr {
    private final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    private static final Logger log = LoggerFactory.getLogger(Qr.class);

    public static Bitmap bitmap(@Nonnull final String content, final int size) {
        return bitmap(content, size, Color.BLACK, Color.TRANSPARENT);
    }

    public static Bitmap bitmap(@Nonnull final String content, final int size, int fgColor, int bgColor) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            final BitMatrix result = QR_CODE_WRITER.encode(content,
                    BarcodeFormat.QR_CODE, size, size, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? fgColor
                            : bgColor;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (final WriterException x) {
            log.info("problem creating qr code", x);
            return null;
        }
    }

    public static String encodeBinary(@Nonnull final byte[] bytes) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(
                    bytes.length);
            final GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(bytes);
            gos.close();

            final byte[] gzippedBytes = bos.toByteArray();
            final boolean useCompressioon = gzippedBytes.length < bytes.length;

            final StringBuilder str = new StringBuilder();
            str.append(useCompressioon ? 'Z' : '-');
            str.append(Base43.encode(useCompressioon ? gzippedBytes : bytes));

            return str.toString();
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }

    public static byte[] decodeBinary(@Nonnull final String content)
            throws IOException {
        final boolean useCompression = content.charAt(0) == 'Z';
        final byte[] bytes = Base43.decode(content.substring(1));

        InputStream is = new ByteArrayInputStream(bytes);
        if (useCompression)
            is = new GZIPInputStream(is);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final byte[] buf = new byte[4096];
        int read;
        while (-1 != (read = is.read(buf)))
            baos.write(buf, 0, read);
        baos.close();
        is.close();

        return baos.toByteArray();
    }

    public static void printQrContentSize() {
        for (int versionNum = 1; versionNum <= 40; versionNum++) {
            Version version = Version.getVersionForNumber(versionNum);
            // numBytes = 196
            int numBytes = version.getTotalCodewords();
            // getNumECBytes = 130
            Version.ECBlocks ecBlocks = version
                    .getECBlocksForLevel(ErrorCorrectionLevel.L);
            int numEcBytes = ecBlocks.getTotalECCodewords();
            // getNumDataBytes = 196 - 130 = 66
            int numDataBytes = numBytes - numEcBytes;
            int numInputBytes = numDataBytes * 8 - 7;
            int length = (numInputBytes - 10) / 11 * 2;
            LogUtil.d("Qr", "Version: " + versionNum + " numData bytes: "
                    + numDataBytes + "  input: " + numInputBytes
                    + "  string length: " + length);
        }
    }
}
