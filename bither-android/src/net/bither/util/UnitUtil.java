/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.preference.AppSharedPreference;

/**
 * Created by songchenwen on 14-11-11.
 */
public class UnitUtil {
    public static enum BitcoinUnit {
        BTC(100000000, 2, R.drawable.symbol_btc_slim, R.drawable.symbol_btc), bits(100, 0,
                R.drawable.symbol_bits_slim, R.drawable.symbol_bits);

        public long satoshis;
        public int slimDrawable;
        public int boldDrawable;
        public int boldAfterDot;
        private Bitmap bmpSlim;
        private Bitmap bmp;

        BitcoinUnit(long satoshis, int boldAfterDot, int slimDrawable, int boldDrawable) {
            this.satoshis = satoshis;
            this.slimDrawable = slimDrawable;
            this.boldDrawable = boldDrawable;
            this.boldAfterDot = boldAfterDot;
        }

        public Bitmap getBmpSlim() {
            if (bmpSlim == null) {
                bmpSlim = BitmapFactory.decodeResource(BitherApplication.mContext.getResources(),
                        slimDrawable);
            }
            return bmpSlim;
        }

        public Bitmap getBmp() {
            if (bmp == null) {
                bmp = BitmapFactory.decodeResource(BitherApplication.mContext.getResources(),
                        boldDrawable);
            }
            return bmp;
        }
    }

    private static final int MinBlackValue = 0;

    private static BitcoinUnit unit() {
        AppSharedPreference.getInstance().setBitcoinUnit(BitcoinUnit.bits);
        return AppSharedPreference.getInstance().getBitcoinUnit();
    }

    public static String formatValue(final long value) {
        BitcoinUnit unit = unit();
        String sign = value < 0 ? "-" : "";
        long absValue = Math.abs(value);
        long coins = absValue / unit.satoshis;
        long satoshis = absValue % unit.satoshis;
        String strCoins = Long.toString(coins);
        String strSatoshis = "";
        strSatoshis = Long.toString(satoshis + unit.satoshis);
        strSatoshis = strSatoshis.substring(1, strSatoshis.length());
        if (unit.satoshis > Math.pow(10, 2)) {
            strSatoshis = strSatoshis.replaceFirst("[0]{1," + Integer.toString((int) Math.floor
                    (Math.log10(unit.satoshis) - 2)) + "}$", "");
        }
        return sign + strCoins + (strSatoshis.length() > 0 ? "." : "") + strSatoshis;
    }

    public static SpannableString formatValueWithBold(final long value) {
        return formatValueWithBold(value, unit().boldAfterDot);
    }

    private static SpannableString formatValueWithBold(final long value, int boldLengthAfterDot) {
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

    public static Bitmap getBtcSymbol(TextView tv) {
        return getBtcSymbol(tv, unit());
    }

    public static Bitmap getBtcSymbol(TextView tv, BitcoinUnit unit) {
        return getBtcSymbol(adjustTextColor(tv.getTextColors().getDefaultColor()),
                tv.getTextSize(), unit);
    }

    public static Bitmap getBtcSymbol(int color) {
        return getBtcSymbol(color, 0, unit());
    }

    public static Bitmap getBtcSymbol(int color, float textSize, BitcoinUnit unit) {
        Bitmap bmp = scaleToTextSize(unit.getBmp(), textSize);
        if (color == Color.WHITE) {
            return bmp;
        }
        return changeBitmapColor(bmp, color);
    }

    public static Bitmap getBtcSlimSymbol(TextView tv) {
        return getBtcSlimSymbol(tv, unit());
    }

    public static Bitmap getBtcSlimSymbol(TextView tv, BitcoinUnit unit) {
        return getBtcSlimSymbol(adjustTextColor(tv.getTextColors().getDefaultColor()),
                tv.getTextSize(), unit);
    }

    public static Bitmap getBtcSlimSymbol(int color) {
        return getBtcSlimSymbol(color, 0, unit());
    }

    public static Bitmap getBtcSlimSymbol(int color, float textSize, BitcoinUnit unit) {
        Bitmap bmp = scaleToTextSize(unit.getBmpSlim(), textSize);
        if (color == Color.WHITE) {
            return bmp;
        }
        return changeBitmapColor(bmp, color);
    }

    private static Bitmap scaleToTextSize(Bitmap bmp, float textSize) {
        int bmpHeight = bmp.getHeight();
        if (textSize > 0 && textSize != bmp.getHeight()) {
            float scale = textSize / (float) bmpHeight;
            bmp = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * scale),
                    (int) (bmpHeight * scale), true);
        }
        return bmp;
    }

    public static Bitmap changeBitmapColor(Bitmap bmp, int color) {
        Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas c = new Canvas(result);
        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 1);
        paint.setColorFilter(filter);
        c.drawBitmap(bmp, 0, 0, paint);
        return result;
    }

    private static int adjustTextColor(int color) {
        if (Color.red(color) == Color.green(color) && Color.green(color) == Color.blue(color)) {
            int value = Color.red(color);
            if (value < MinBlackValue) {
                value = MinBlackValue;
                return Color.argb(Color.alpha(color), value, value, value);
            }
        }
        return color;
    }

    public static void test(Context context) {
        Dialog d = new Dialog(context);
        TextView tv = new TextView(context);
        ImageView iv = new ImageView(context);
        tv.setTextSize(20);
        tv.setText("100.00");
        iv.setImageBitmap(getBtcSymbol(Color.RED, tv.getTextSize(), unit()));
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.addView(iv);
        ll.addView(tv);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
        lp.gravity = Gravity.CENTER_VERTICAL;
        d.setContentView(ll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        d.show();
    }
}
