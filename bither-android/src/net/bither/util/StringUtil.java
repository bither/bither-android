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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;

import net.bither.BitherApplication;
import net.bither.bitherj.exception.ScriptException;
import net.bither.bitherj.script.Script;
import net.bither.bitherj.utils.Utils;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    public static boolean checkAddressIsNull(String addressStr) {
        byte[] EMPTY_BYTES = new byte[32];
        String address;
        try {
            address = new Script(Script.createInputScript(EMPTY_BYTES, EMPTY_BYTES)).getToAddress();
            return Utils.compareString(addressStr, address);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return false;

    }


    public static String makeFragmentName(int paramInt1, int paramInt2) {
        return "android:switcher:" + paramInt1 + ":" + paramInt2;
    }


    @SuppressLint("NewApi")
    public static void copyString(String text) {
        if (android.os.Build.VERSION.SDK_INT > 10) {
            android.content.ClipboardManager clip = (android.content.ClipboardManager)
                    BitherApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText(text, text));
        } else {
            android.text.ClipboardManager clipM = (android.text.ClipboardManager)
                    BitherApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            clipM.setText(text);
        }
    }

    public static boolean checkBackupFileOfCold(String fileName) {
        Pattern pattern = Pattern.compile("[^-]{6,6}_[^-]{6,6}.bak");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

}
