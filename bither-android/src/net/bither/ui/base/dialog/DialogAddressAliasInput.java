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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import net.bither.R;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.utils.Utils;
import net.bither.util.ThreadUtil;

/**
 * Created by songchenwen on 15/3/3.
 */
public class DialogAddressAliasInput extends CenterDialog implements View.OnClickListener,
        DialogInterface.OnShowListener {
    private Address address;
    private EditText et;
    private DialogAddressAlias.DialogAddressAliasDelegate delegate;
    private InputMethodManager imm;

    public DialogAddressAliasInput(Context context, Address address,
                                   DialogAddressAlias.DialogAddressAliasDelegate delegate) {
        super(context);
        setContentView(R.layout.dialog_address_alias_input);
        this.address = address;
        this.delegate = delegate;
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        et = (EditText) findViewById(R.id.et);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
        setOnShowListener(this);
    }

    @Override
    public void show() {
        if (!Utils.isEmpty(address.getAlias())) {
            et.setText(address.getAlias());
        } else {
            et.setText("");
        }
        super.show();
    }

    @Override
    public void onClick(View v) {
        dismiss();
        if (v.getId() == R.id.btn_ok) {
            String alias = et.getText().toString();
            if (!Utils.isEmpty(alias)) {
                address.updateAlias(alias);
                if (delegate != null) {
                    delegate.onAddressAliasChanged(address, alias);
                }
            } else {
                if (Utils.isEmpty(address.getAlias())) {
                    return;
                }
                new DialogConfirmTask(getContext(), getContext().getString(R.string
                        .address_alias_remove_confirm), new Runnable() {
                    @Override
                    public void run() {
                        address.removeAlias();
                        ThreadUtil.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (delegate != null) {
                                    delegate.onAddressAliasChanged(address, null);
                                }
                            }
                        });
                    }
                }).show();
                address.removeAlias();
            }
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        imm.showSoftInput(et, 0);
    }
}
