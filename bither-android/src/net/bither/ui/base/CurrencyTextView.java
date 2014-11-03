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

package net.bither.ui.base;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.GenericUtils;
import net.bither.util.WalletUtils;

import java.math.BigInteger;

import javax.annotation.Nonnull;

public final class CurrencyTextView extends TextView {
    public static final char CHAR_THIN_SPACE = '\u2009';
    public static final char CHAR_HAIR_SPACE = '\u200a';
    public static final String CURRENCY_PLUS_SIGN = "+" + CHAR_THIN_SPACE;
    public static final String CURRENCY_MINUS_SIGN = "-" + CHAR_THIN_SPACE;
    private String prefix = null;
    private ForegroundColorSpan prefixColorSpan = null;
    private BigInteger amount = null;
    private int precision = 0;
    private int shift = 0;
    private boolean alwaysSigned = false;
    private RelativeSizeSpan prefixRelativeSizeSpan = null;
    private RelativeSizeSpan insignificantRelativeSizeSpan = null;

    public CurrencyTextView(final Context context) {
        super(context);
    }

    public CurrencyTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPrefix(@Nonnull final String prefix) {
        this.prefix = prefix + CHAR_HAIR_SPACE;
        updateView();
    }

    public void setPrefixColor(final int prefixColor) {
        this.prefixColorSpan = new ForegroundColorSpan(prefixColor);
        updateView();
    }

    public void setAmount(@Nonnull final BigInteger amount) {
        this.amount = amount;
        updateView();
    }

    public void setPrecision(final int precision, final int shift) {
        this.precision = precision;
        this.shift = shift;
        updateView();
    }

    public void setAlwaysSigned(final boolean alwaysSigned) {
        this.alwaysSigned = alwaysSigned;
        updateView();
    }

    public void setStrikeThru(final boolean strikeThru) {
        if (strikeThru)
            setPaintFlags(getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            setPaintFlags(getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    public void setInsignificantRelativeSize(
            final float insignificantRelativeSize) {
        if (insignificantRelativeSize != 1) {
            this.prefixRelativeSizeSpan = new RelativeSizeSpan(
                    insignificantRelativeSize);
            this.insignificantRelativeSizeSpan = new RelativeSizeSpan(
                    insignificantRelativeSize);
        } else {
            this.prefixRelativeSizeSpan = null;
            this.insignificantRelativeSizeSpan = null;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setPrefixColor(getResources().getColor(R.color.fg_less_significant));
        setInsignificantRelativeSize(0.85f);
        setSingleLine();
    }

    private void updateView() {
        final Editable text;

        if (amount != null) {
            final String s;
            if (alwaysSigned)
                s = GenericUtils.formatValue(amount.longValue(), CURRENCY_PLUS_SIGN,
                        CURRENCY_MINUS_SIGN, precision, shift);
            else
                s = GenericUtils.formatValue(amount.longValue(), precision, shift);

            text = new SpannableStringBuilder(s);
            WalletUtils.formatSignificant(text, insignificantRelativeSizeSpan);

            if (prefix != null) {
                text.insert(0, prefix);
                if (prefixRelativeSizeSpan != null)
                    text.setSpan(prefixRelativeSizeSpan, 0, prefix.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (prefixColorSpan != null)
                    text.setSpan(prefixColorSpan, 0, prefix.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            text = null;
        }

        setText(text);
    }
}
