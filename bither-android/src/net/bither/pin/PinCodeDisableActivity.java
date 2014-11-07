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
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightActivity;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by songchenwen on 14-11-6.
 */
public class PinCodeDisableActivity extends SwipeRightActivity implements PinCodeEnterView
        .PinCodeEnterViewListener {
    private PinCodeEnterView pv;
    private AppSharedPreference p = AppSharedPreference.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_code_setting);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.pin_code_setting_close);
        pv = (PinCodeEnterView) findViewById(R.id.pv);
        pv.setListener(this);
    }

    @Override
    public void onEntered(CharSequence code) {
        if (code == null || code.length() == 0) {
            return;
        }
        if (p.checkPinCode(code)) {
            p.deletePinCode();
            finish();
            overridePendingTransition(0, R.anim.slide_out_right);
        } else {
            pv.shakeToClear();
            DropdownMessage.showDropdownMessage(this, R.string.pin_code_setting_close_wrong);
        }
    }
}
