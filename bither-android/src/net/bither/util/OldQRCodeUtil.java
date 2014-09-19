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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldQRCodeUtil {
    public static final String OLD_QR_CODE_SPLIT = ":";

    private static final String OLD_QR_CODE_LETTER = "*";

    private static final int MAX_QRCODE_SIZE = 328;

    public static String[] splitOldString(String str) {
        String[] stringArray = str.split(OldQRCodeUtil.OLD_QR_CODE_SPLIT);
        return stringArray;
    }

    public static String encodeQrCodeString(String text) {
        Pattern pattern = Pattern.compile("[A-Z]");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String letter = matcher.group(0);
            matcher.appendReplacement(sb, OLD_QR_CODE_LETTER + letter);
        }
        matcher.appendTail(sb);

        return sb.toString().toUpperCase(Locale.US);
    }

    public static String decodeQrCodeString(String formatString) {
        formatString = formatString.toLowerCase(Locale.US);
        Pattern pattern = Pattern.compile("\\*([a-z])");
        Matcher matcher = pattern.matcher(formatString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String letter = matcher.group(1);
            matcher.appendReplacement(sb, letter.toUpperCase(Locale.US));
        }
        matcher.appendTail(sb);
        return sb.toString();

    }

    public static boolean verifyQrcodeTransport(String text) {
        Pattern pattern = Pattern.compile("[^0-9A-Z\\*:]");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return false;
        }
        return true;
    }

    public static List<String> getQrCodeStringList(String str) {
        List<String> stringList = new ArrayList<String>();
        int num = getNumOfQrCodeString(str.length());
        int sumLength = str.length() + num * 6;
        int pageSize = sumLength / num;
        for (int i = 0;
             i < num;
             i++) {
            int start = i * pageSize;
            int end = (i + 1) * pageSize;
            LogUtil.d("qr", "s:" + start + " e:" + end);
            if (start > str.length() - 1) {
                continue;
            }
            if (end > str.length()) {
                end = str.length();
            }
            String splitStr = str.substring(start, end);
            String pageString = "";
            if (num > 1) {
                pageString = Integer.toString(num - 1) + OLD_QR_CODE_SPLIT
                        + Integer.toString(i) + OLD_QR_CODE_SPLIT;
            }
            stringList.add(pageString + splitStr);
        }
        return stringList;
    }

    private static int getNumOfQrCodeString(int length) {
        if (length < MAX_QRCODE_SIZE) {
            return 1;
        } else if (length <= (MAX_QRCODE_SIZE - 4) * 10) {
            return length / (MAX_QRCODE_SIZE - 4) + 1;
        } else if (length <= (MAX_QRCODE_SIZE - 5) * 100) {
            return length / (MAX_QRCODE_SIZE - 5) + 1;
        } else if (length <= (MAX_QRCODE_SIZE - 6) * 1000) {
            return length / (MAX_QRCODE_SIZE - 6) + 1;
        } else {
            return 1000;
        }

    }
}
