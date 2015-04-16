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

package net.bither.activity.hot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import net.bither.BitherSetting;
import net.bither.R;
import net.bither.fragment.hot.AddAddressHotHDMFragment;
import net.bither.ui.base.AddPrivateKeyActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.listener.IBackClickListener;

import java.util.ArrayList;

/**
 * Created by songchenwen on 15/4/16.
 */
public class AddHotAddressHDMActivity extends AddPrivateKeyActivity {
    private AddAddressHotHDMFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_hot_address_hdm);
        fragment = (AddAddressHotHDMFragment) getSupportFragmentManager().findFragmentById(R.id
                .fragment);
        findViewById(R.id.ibtn_cancel).setOnClickListener(new IBackClickListener());
    }

    @Override
    public void save() {
        ArrayList<String> addresses = fragment.getAddresses();
        Intent intent = new Intent();
        intent.putExtra(BitherSetting.INTENT_REF.ADDRESS_POSITION_PASS_VALUE_TAG, addresses);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }


    @Override
    public void finish() {
        if (fragment.canCancel()) {
            super.finish();
            overridePendingTransition(0, R.anim.slide_out_bottom);
        } else {
            DropdownMessage.showDropdownMessage(this, R.string.hdm_singular_mode_cancel_warn);
        }
    }
}
