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
import android.view.View;
import android.widget.CheckBox;

import net.bither.R;

/**
 * Created by songchenwen on 15/1/15.
 */
public class DialogHdmKeychainAddHot extends CenterDialog implements View.OnClickListener,
        DialogInterface.OnDismissListener {
    public static interface DialogHdmKeychainAddHotDelegate {
        public void addWithXRandom();

        public void addWithoutXRandom();
    }

    private DialogHdmKeychainAddHotDelegate delegate;

    private int clickedId;
    private CheckBox cbxXrandom;

    public DialogHdmKeychainAddHot(Context context, DialogHdmKeychainAddHotDelegate delegate) {
        super(context);
        this.delegate = delegate;
        setContentView(R.layout.dialog_hdm_keychain_add_hot_confirm);
        setOnDismissListener(this);
        cbxXrandom = (CheckBox) findViewById(R.id.cbx_xrandom);
        findViewById(R.id.tv_ok).setOnClickListener(this);
        findViewById(R.id.tv_cancel).setOnClickListener(this);
    }

    @Override
    public void show() {
        super.show();
        clickedId = 0;
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedId == R.id.tv_ok && delegate != null) {
            if (cbxXrandom.isChecked()) {
                delegate.addWithXRandom();
            } else {
                delegate.addWithoutXRandom();
            }
        }
    }
}
