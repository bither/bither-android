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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.R;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.utils.GenericUtils;
import net.bither.preference.AppSharedPreference;
import net.bither.util.UIUtil;
import net.bither.util.UnitUtil;
import net.bither.util.WalletUtils;

import java.math.BigInteger;
import java.util.Currency;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CurrencyAmountView extends FrameLayout {
    public static interface Listener {
        void changed();

        void done();

        void focusChanged(final boolean hasFocus);
    }

    private int significantColor, lessSignificantColor, errorColor;
    private Drawable deleteButtonDrawable, contextButtonDrawable;
    private Drawable currencySymbolDrawable;
    private int inputPrecision = 0;
    private int hintPrecision = 0;
    private int shift = 0;
    private boolean amountSigned = false;
    private boolean smallerInsignificant = true;
    private boolean validateAmount = true;

    private TextView textView;
    private View contextButton;
    private Listener listener;
    private OnClickListener contextButtonClickListener;

    public CurrencyAmountView(final Context context) {
        super(context);
        init(context);
    }

    public CurrencyAmountView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {
        final Resources resources = context.getResources();
        significantColor = Color.BLACK;
        lessSignificantColor = Color.parseColor("#ff666666");
        errorColor = Color.RED;
        deleteButtonDrawable = resources.getDrawable(R.drawable.ic_input_delete);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        textView = (TextView) getChildAt(0);
        textView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        textView.setHintTextColor(lessSignificantColor);
        textView.setHorizontalFadingEdgeEnabled(true);
        textView.setSingleLine();
        textView.setCompoundDrawablePadding(UIUtil.dip2pix(2));
        setHint(0);
        setValidateAmount(textView instanceof EditText);
        textView.addTextChangedListener(textViewListener);
        textView.setOnFocusChangeListener(textViewListener);
        textView.setOnEditorActionListener(textViewListener);

        contextButton = new View(context) {
            @Override
            protected void onMeasure(final int wMeasureSpec, final int hMeasureSpec) {
                setMeasuredDimension(textView.getCompoundPaddingRight(),
                        textView.getMeasuredHeight());
            }
        };
        final LayoutParams chooseViewParams = new LayoutParams(ViewGroup.LayoutParams
                .WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chooseViewParams.gravity = Gravity.RIGHT;
        contextButton.setLayoutParams(chooseViewParams);
        this.addView(contextButton);

        updateAppearance();
    }

    public void setCurrencySymbol(@Nullable final String currencyCode) {
        if (BitherApplication.mContext.getString(R.string.bitcoin_symbol).equals(currencyCode)) {
            Bitmap bmp = UnitUtil.getBtcSymbol(lessSignificantColor, textView.getTextSize(),
                    AppSharedPreference.getInstance().getBitcoinUnit());
            currencySymbolDrawable = new BitmapDrawable(getResources(), bmp);
            currencySymbolDrawable.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
        } else if (currencyCode != null) {
            final String currencySymbol = currencySymbol(currencyCode);
            final float textSize = textView.getTextSize();
            final float smallerTextSize = textSize * (smallerInsignificant ? (20f / 24f) : 1);
            currencySymbolDrawable = new CurrencySymbolDrawable(currencySymbol, smallerTextSize,
                    lessSignificantColor, textSize * 0.37f);
        } else {
            currencySymbolDrawable = null;
        }

        updateAppearance();
    }

    public void setInputPrecision(final int inputPrecision) {
        this.inputPrecision = inputPrecision;
    }

    public void setHintPrecision(final int hintPrecision) {
        this.hintPrecision = hintPrecision;
    }

    public void setShift(final int shift) {
        this.shift = shift;
    }

    public void setAmountSigned(final boolean amountSigned) {
        this.amountSigned = amountSigned;
    }

    public void setSmallerInsignificant(final boolean smallerInsignificant) {
        this.smallerInsignificant = smallerInsignificant;
    }

    public void setValidateAmount(final boolean validateAmount) {
        this.validateAmount = validateAmount;
    }

    public void setContextButton(final int contextButtonResId, @Nonnull final OnClickListener
            contextButtonClickListener) {
        this.contextButtonDrawable = getContext().getResources().getDrawable(contextButtonResId);
        this.contextButtonClickListener = contextButtonClickListener;

        updateAppearance();
    }

    public void setListener(@Nonnull final Listener listener) {
        this.listener = listener;
    }

    @CheckForNull
    public long getAmount() {
        if (isValidAmount(false)) {
            return GenericUtils.toNanoCoins(textView.getText().toString().trim(), shift).longValue();
        } else {
            return 0;
        }
    }

    public void setAmount(@Nullable final long amount, final boolean fireListener) {
        if (!fireListener) {
            textViewListener.setFire(false);
        }

        if (amount != 0) {
            textView.setText(amountSigned ? GenericUtils.formatValue(amount,
                    BitherSetting.CURRENCY_PLUS_SIGN, BitherSetting.CURRENCY_MINUS_SIGN,
                    inputPrecision, shift) : GenericUtils.formatValue(amount, inputPrecision,
                    shift));
        } else {
            textView.setText(null);
        }

        if (!fireListener) {
            textViewListener.setFire(true);
        }
    }

    public void setHint(@Nullable final long amount) {
        final SpannableStringBuilder hint;
        if (amount != 0) {
            hint = new SpannableStringBuilder(GenericUtils.formatValue(amount, hintPrecision,
                    shift));
        } else {
            hint = new SpannableStringBuilder("0.00");
        }

        WalletUtils.formatSignificant(hint, smallerInsignificant ? WalletUtils.SMALLER_SPAN : null);
        textView.setHint(hint);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        textView.setEnabled(enabled);

        updateAppearance();
    }

    public void setTextColor(final int color) {
        significantColor = color;

        updateAppearance();
    }

    public void setStrikeThru(final boolean strikeThru) {
        if (strikeThru) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private boolean isValidAmount(final boolean zeroIsValid) {
        final String amount = textView.getText().toString().trim();

        try {
            if (!amount.isEmpty()) {
                final BigInteger nanoCoins = GenericUtils.toNanoCoins(amount, shift);

                // exactly zero
                if (zeroIsValid && nanoCoins.signum() == 0) {
                    return true;
                }

                // too small
                if (nanoCoins.longValue() < Tx.MIN_NONDUST_OUTPUT) {
                    return false;
                }

                return true;
            }
        } catch (final Exception x) {
        }

        return false;
    }

    private final OnClickListener deleteClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            setAmount(0, true);
            textView.requestFocus();
        }
    };

    private void updateAppearance() {
        final boolean enabled = textView.isEnabled();

        contextButton.setEnabled(enabled);

        final String amount = textView.getText().toString().trim();

        if (enabled && !amount.isEmpty()) {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null,
                    deleteButtonDrawable, null);
            contextButton.setOnClickListener(deleteClickListener);
        } else if (enabled && contextButtonDrawable != null) {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null,
                    contextButtonDrawable, null);
            contextButton.setOnClickListener(contextButtonClickListener);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, null,
                    null);
            contextButton.setOnClickListener(null);
        }

        contextButton.requestLayout();

        textView.setTextColor(!validateAmount || isValidAmount(true) ? significantColor :
                errorColor);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable("super_state", super.onSaveInstanceState());
        state.putParcelable("child_textview", textView.onSaveInstanceState());
        state.putLong("amount", getAmount());
        return state;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("super_state"));
            textView.onRestoreInstanceState(bundle.getParcelable("child_textview"));
            setAmount(bundle.getLong("amount"), false);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private final TextViewListener textViewListener = new TextViewListener();

    private final class TextViewListener implements TextWatcher, OnFocusChangeListener,
            OnEditorActionListener {
        private boolean fire = true;

        public void setFire(final boolean fire) {
            this.fire = fire;
        }

        @Override
        public void afterTextChanged(final Editable s) {
            // workaround for German keyboards
            final String original = s.toString();
            final String replaced = original.replace(',', '.');
            if (!replaced.equals(original)) {
                s.clear();
                s.append(replaced);
            }

            WalletUtils.formatSignificant(s, smallerInsignificant ? WalletUtils.SMALLER_SPAN :
                    null);
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            updateAppearance();
            if (listener != null && fire) {
                listener.changed();
            }
        }

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                final long amount = getAmount();
                if (amount != 0) {
                    setAmount(amount, false);
                }
            }

            if (listener != null && fire) {
                listener.focusChanged(hasFocus);
            }
        }

        @Override
        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE && listener != null && fire) {
                listener.done();
            }

            return false;
        }
    }

    private static String currencySymbol(@Nonnull final String currencyCode) {
        try {
            final Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol();
        } catch (final IllegalArgumentException x) {
            return currencyCode;
        }
    }
}
