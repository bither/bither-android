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

package net.bither;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import net.bither.ui.base.keyboard.pin.PinEntryKeyboardView;
import net.bither.util.LogUtil;

/**
 * Created by songchenwen on 14-11-5.
 */
public class PinCodeActivity extends Activity implements TextWatcher,
        PinEntryKeyboardView.PinEntryKeyboardViewListener {
    private EditText et;
    private PinEntryKeyboardView kv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_code);
        initView();
    }

    private void initView() {
        et = (EditText) findViewById(R.id.et);
        kv = (PinEntryKeyboardView) findViewById(R.id.kv);
        kv.setListener(this);
        kv.registerEditText(et);
        et.addTextChangedListener(this);
        et.requestFocus();
    }

    @Override
    public void afterTextChanged(Editable s) {
        LogUtil.i(PinCodeActivity.class.getSimpleName(), "text changed " + s);
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
}
