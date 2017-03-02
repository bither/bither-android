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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.utils.Utils;
import net.bither.mnemonic.MnemonicCodeAndroid;

import java.io.IOException;

/**
 * Created by songchenwen on 15/1/23.
 */
public class DialogHdmImportWordListReplace extends CenterDialog implements DialogInterface
        .OnShowListener, View.OnClickListener, TextView.OnEditorActionListener {
    public static interface DialogHdmImportWordListReplaceListener {
        public void replace(int index, String word);
    }

    private Activity activity;
    private EditText et;
    private TextView tvError;
    private InputMethodManager imm;
    private int index;
    private DialogHdmImportWordListReplaceListener listener;

    public DialogHdmImportWordListReplace(Activity context, int index,
                                          DialogHdmImportWordListReplaceListener listener) {
        super(context);
        this.activity = context;
        this.index = index;
        this.listener = listener;
        setContentView(R.layout.dialog_hdm_import_word_list_replace);
        et = (EditText) findViewById(R.id.et);
        tvError = (TextView) findViewById(R.id.tv_error);
        et.addTextChangedListener(textWatcher);
        et.setOnEditorActionListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        setOnShowListener(this);
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void show() {
        et.setText("");
        tvError.setVisibility(View.GONE);
        super.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            String word = et.getText().toString().toLowerCase().trim();
            if (Utils.isEmpty(word)) {
                return;
            }
            try {
                MnemonicCode mnemonic = MnemonicCode.instanceForWord(new MnemonicCodeAndroid(word));
                if (mnemonic.getWordList() == null) {
                    tvError.setVisibility(View.VISIBLE);
                    shake();
                    return;
                }
                dismiss();
                if (listener != null) {
                    listener.replace(index, word);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            dismiss();
        }
    }


    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(et, 0);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            tvError.setVisibility(View.GONE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        onClick(findViewById(R.id.btn_ok));
        return true;
    }
}
