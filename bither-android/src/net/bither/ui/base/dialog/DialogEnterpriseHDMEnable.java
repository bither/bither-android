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
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import net.bither.R;
import net.bither.activity.cold.EnterpriseHDMSeedActivity;
import net.bither.activity.hot.EnterpriseHDMKeychainActivity;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.utils.Sha256Hash;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;

import java.util.Arrays;

/**
 * Created by songchenwen on 15/6/17.
 */
public class DialogEnterpriseHDMEnable extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener {
    private static final String CodeHash =
            "F7C651DE4C1C37BAB7F0CDE32B44DF0884CE0113F70AF70E23601A5AE0C6C9D0";

    private Activity activity;

    private EditText et;

    private int clickedId;

    public DialogEnterpriseHDMEnable(Activity context) {
        super(context);
        this.activity = context;
        setContentView(R.layout.dialog_enterprise_hdm_enable);
        setOnDismissListener(this);
        initView();
    }

    private void initView() {
        et = (EditText) findViewById(R.id.et);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_ok).setOnClickListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedId == R.id.btn_ok) {
            Sha256Hash hash = Sha256Hash.create(et.getText().toString().trim().getBytes());
            if (Arrays.equals(hash.getBytes(), Utils.hexStringToByteArray(CodeHash))) {
                if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.HOT) {
                    activity.startActivity(new Intent(activity, EnterpriseHDMKeychainActivity
                            .class));
                } else {
                    activity.startActivity(new Intent(activity, EnterpriseHDMSeedActivity.class));
                }
            } else {
                DropdownMessage.showDropdownMessage(activity, R.string
                        .enterprise_hdm_keychain_enable_failed);
            }
        }
    }

    @Override
    public void show() {
        super.show();
        clickedId = 0;
        et.requestFocus();
    }

    @Override
    public void onClick(View v) {
        clickedId = v.getId();
        dismiss();
    }
}
