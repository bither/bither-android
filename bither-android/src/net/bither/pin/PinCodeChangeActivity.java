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

package net.bither.pin;

import android.os.Bundle;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by songchenwen on 14-11-6.
 */
public class PinCodeChangeActivity extends SwipeRightActivity implements PinCodeEnterView
        .PinCodeEnterViewListener {
    private PinCodeEnterView pv;
    private AppSharedPreference p = AppSharedPreference.getInstance();
    private CharSequence firstPin;
    private boolean passedOld = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_code_setting);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.pin_code_setting_change);
        pv = (PinCodeEnterView) findViewById(R.id.pv);
        pv.setListener(this);
        pv.setMessage(R.string.pin_code_setting_change_old_msg);
    }

    @Override
    public void onEntered(CharSequence code) {
        if (code == null || code.length() == 0) {
            return;
        }
        if (!passedOld) {
            if (p.checkPinCode(code)) {
                passedOld = true;
                pv.animateToNext();
                pv.setMessage(R.string.pin_code_setting_change_new_msg);
            } else {
                passedOld = false;
                pv.shakeToClear();
                DropdownMessage.showDropdownMessage(this,
                        R.string.pin_code_setting_change_old_wrong);
            }
            return;
        }
        if (firstPin == null) {
            firstPin = code;
            pv.animateToNext();
            pv.setMessage(R.string.pin_code_setting_change_new_repeat_msg);
        } else {
            if (Utils.compareString(firstPin.toString(), code.toString())) {
                p.setPinCode(code);
                finish();
                overridePendingTransition(0, R.anim.slide_out_right);
            } else {
                DropdownMessage.showDropdownMessage(this,
                        R.string.pin_code_setting_change_repeat_wrong);
                pv.setMessage(R.string.pin_code_setting_change_new_msg);
                pv.shakeToClear();
                firstPin = null;
            }
        }
    }
}
