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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.Address;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.AbstractWheelTextAdapter;

/**
 * Created by songchenwen on 15/5/6.
 */
public class DialogEditVanityLength extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener, OnWheelChangedListener {
    private int clickedId;
    private Address address;
    private TextView tvAddress;
    private WheelView wvLength;

    public DialogEditVanityLength(Context context, Address address) {
        super(context);
        this.address = address;
        setContentView(R.layout.dialog_edit_vanity_length);
        setOnDismissListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        wvLength = (WheelView) findViewById(R.id.wv_length);
        wvLength.setViewAdapter(lengthAdapter);
        wvLength.setCurrentItem(address.exsitsVanityLen() ? Math.max(address.getVanityLen() - 1,
                0) : 0);
        wvLength.addChangingListener(this);
        showVanityLength(address.getVanityLen());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedId == R.id.btn_ok) {
            int length = getLengthFromWheelIndex(wvLength.getCurrentItem());
            if (length > 0) {
                address.updateVanityLen(length);
            } else {
                address.removeVanitylen();
            }
        }
    }

    @Override
    public void onChanged(WheelView wheel, int oldValue, int newValue) {
        showVanityLength(getLengthFromWheelIndex(newValue));
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    private SpannableStringBuilder spannable;
    private int currentLength;
    private ShadowSpan shadow;
    private ForegroundColorSpan color;

    private void showVanityLength(int length) {
        if (currentLength == length) {
            return;
        }
        if (length > 0) {
            float radiusRate = 0.36f;
            float dxRate = 0f;
            float dyRate = 0f;
            float size = tvAddress.getTextSize();
            if (spannable == null) {
                spannable = new SpannableStringBuilder(address.getAddress());
            }
            if (shadow == null) {
                shadow = new ShadowSpan(size * radiusRate, size * dxRate, size * dyRate,
                        getContext().getResources().getColor(R.color.vanity_address_glow));
            }
            if (color == null) {
                color = new ForegroundColorSpan(getContext().getResources().getColor(R.color
                        .vanity_address_text));
            }

            spannable.setSpan(shadow, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(color, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvAddress.setText(spannable);

        } else {
            tvAddress.setText(address.getAddress());
        }
        currentLength = length;
    }

    private int getLengthFromWheelIndex(int index) {
        if (index > 0) {
            return Math.min(index + 1, address.getAddress().length());
        }
        return 0;
    }

    private AbstractWheelTextAdapter lengthAdapter = new AbstractWheelTextAdapter(getContext()) {

        @Override
        public int getItemsCount() {
            return address.getAddress().length();
        }

        @Override
        protected CharSequence getItemText(int index) {
            int length = getLengthFromWheelIndex(index);
            if (length > 0) {
                return String.valueOf(length);
            }
            return getContext().getString(R.string.vanity_address_none);
        }
    };

    private class ShadowSpan extends CharacterStyle {
        private float dx;
        private float dy;
        private float radius;
        private int color;

        public ShadowSpan(float radius, float dx, float dy, int color) {
            this.radius = radius;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setShadowLayer(radius, dx, dy, color);
        }
    }
}
