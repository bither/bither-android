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

package net.bither.ui.base;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.ui.base.keyboard.pin.PinEntryKeyboardView;

/**
 * Created by songchenwen on 14-11-5.
 */
public class PinCodeEnterView extends FrameLayout implements TextWatcher,
        PinEntryKeyboardView.PinEntryKeyboardViewListener {
    public static interface PinCodeEnterViewListener {
        public void onEntered(CharSequence code);
    }

    private int pinCodeLength;
    private EditText et;
    public PinEntryKeyboardView kv;
    public PinCodeDotsView dv;
    public TextView tv;
    private PinCodeEnterViewListener listener;

    public PinCodeEnterView(Context context) {
        super(context);
        initView();
    }

    public PinCodeEnterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinCodeEnterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        removeAllViews();
        LayoutInflater.from(getContext()).inflate(R.layout.layout_pin_code_enter, this, true);
        tv = (TextView) findViewById(R.id.tv);
        et = (EditText) findViewById(R.id.et);
        kv = (PinEntryKeyboardView) findViewById(R.id.kv);
        dv = (PinCodeDotsView) findViewById(R.id.dv);
        dv.setDotColor(getResources().getColor(R.color.pin_code_dot_color));
        kv.setListener(this);
        kv.registerEditText(et);
        et.addTextChangedListener(this);
        et.requestFocus();
        setPinCodeLength(4);
    }

    @Override
    public void afterTextChanged(Editable s) {
        dv.setFilledCount(s.length());
        if (s.length() >= getPinCodeLength()) {
            if (listener != null) {
                listener.onEntered(s);
            }
        }
    }

    @Override
    public void clearText() {
        et.setText("");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public int getPinCodeLength() {
        return pinCodeLength;
    }

    public void setPinCodeLength(int pinCodeLength) {
        this.pinCodeLength = pinCodeLength;
        dv.setTotalDotCount(pinCodeLength);
    }

    public void setListener(PinCodeEnterViewListener listener) {
        this.listener = listener;
    }
}
