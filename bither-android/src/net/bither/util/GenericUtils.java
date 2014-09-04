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

import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import net.bither.bitherj.exception.ScriptException;
import net.bither.bitherj.script.Script;
import net.bither.bitherj.utils.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import javax.annotation.Nonnull;

public class GenericUtils {
    public static final BigInteger ONE_BTC = new BigInteger("100000000", 10);
    public static final BigInteger ONE_MBTC = new BigInteger("100000", 10);

    private static final int ONE_BTC_INT = ONE_BTC.intValue();
    private static final int ONE_MBTC_INT = ONE_MBTC.intValue();

    public static String addressFromScriptPubKey(Script script) {
        byte[] pubKeyHash;
        try {
            if (script.isSentToRawPubKey()) {
                byte[] pubkey;
                pubkey = script.getPubKey();
                pubKeyHash = Utils.sha256hash160(pubkey);
            } else {
                pubKeyHash = script.getPubKeyHash();
            }
        } catch (ScriptException e) {
            e.printStackTrace();
            return null;
        }
        return Utils.toAddress(pubKeyHash);
    }

    public static SpannableString formatValueWithBold(final long value) {
        return formatValueWithBold(value, 2);
    }

    public static SpannableString formatValueWithBold(final long value, int boldLengthAfterDot) {
        String str = formatValue(value);
        int dotPosition = str.indexOf(".");
        int boldLength = str.length();
        if (dotPosition > 0 && dotPosition + boldLengthAfterDot < str.length()) {
            boldLength = dotPosition + boldLengthAfterDot + 1;
        }
        SpannableString spannable = new SpannableString(str);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, boldLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (boldLength < str.length()) {
            spannable.setSpan(new RelativeSizeSpan(0.8f), boldLength, str.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static String formatValue(final long value) {
        String sign = value < 0 ? "-" : "";
        long absValue = Math.abs(value);
        long coins = absValue / ONE_BTC_INT;
        int satoshis = (int) (absValue % ONE_BTC_INT);
        String strCoins = Long.toString(coins);
        String strSatoshis = "";
        strSatoshis = Integer.toString(satoshis + ONE_BTC_INT);
        strSatoshis = strSatoshis.substring(1, strSatoshis.length());
        strSatoshis = strSatoshis.replaceFirst("[0]{1," + (8 - 2) + "}$", "");
        return sign + strCoins + (strSatoshis.length() > 0 ? "." : "") + strSatoshis;
    }

    public static String formatValue(@Nonnull final long value, final int precision,
                                     final int shift) {
        return formatValue(value, "", "-", precision, shift);
    }

    public static String formatValue(@Nonnull long value, @Nonnull final String plusSign,
                                     @Nonnull final String minusSign, final int precision,
                                     final int shift) {

        final String sign = value < 0 ? minusSign : plusSign;

        if (shift == 0) {
            if (precision == 2) {
                value = value - value % 1000000 + value % 1000000 / 500000 * 1000000;
            } else if (precision == 4) {
                value = value - value % 10000 + value % 10000 / 5000 * 10000;
            } else if (precision == 6) {
                value = value - value % 100 + value % 100 / 50 * 100;
            } else if (precision == 8) {
                ;
            } else {
                throw new IllegalArgumentException("cannot handle precision/shift: " + precision
                        + "/" + shift);
            }

            final long absValue = Math.abs(value);
            final long coins = absValue / ONE_BTC_INT;
            final int satoshis = (int) (absValue % ONE_BTC_INT);

            if (satoshis % 1000000 == 0) {
                return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000000);
            } else if (satoshis % 10000 == 0) {
                return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10000);
            } else if (satoshis % 100 == 0) {
                return String.format(Locale.US, "%s%d.%06d", sign, coins, satoshis / 100);
            } else {
                return String.format(Locale.US, "%s%d.%08d", sign, coins, satoshis);
            }
        } else if (shift == 3) {
            if (precision == 2) {
                value = value - value % 1000 + value % 1000 / 500 * 1000;
            } else if (precision == 4) {
                value = value - value % 10 + value % 10 / 5 * 10;
            } else if (precision == 5) {
                ;
            } else {
                throw new IllegalArgumentException("cannot handle precision/shift: " + precision
                        + "/" + shift);
            }

            final long absValue = Math.abs(value);
            final long coins = absValue / ONE_MBTC_INT;
            final int satoshis = (int) (absValue % ONE_MBTC_INT);

            if (satoshis % 1000 == 0) {
                return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000);
            } else if (satoshis % 10 == 0) {
                return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10);
            } else {
                return String.format(Locale.US, "%s%d.%05d", sign, coins, satoshis);
            }
        } else {
            throw new IllegalArgumentException("cannot handle shift: " + shift);
        }
    }

    public static BigInteger toNanoCoins(final String value, final int shift) {
        final BigInteger nanoCoins = new BigDecimal(value).movePointRight(8 - shift)
                .toBigIntegerExact();

        if (nanoCoins.signum() < 0) {
            throw new IllegalArgumentException("negative amount: " + value);
        }

        return nanoCoins;
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
}
