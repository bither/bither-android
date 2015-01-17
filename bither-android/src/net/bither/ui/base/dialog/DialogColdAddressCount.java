/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base.dialog;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.core.AddressManager;

public class DialogColdAddressCount extends DialogWithArrow {
    private TextView tvCount;
    private LinearLayout llHDM;

    public DialogColdAddressCount(Context context) {
        super(context);
        setContentView(R.layout.dialog_cold_address_count);
        tvCount = (TextView) findViewById(R.id.tv_count);
        llHDM = (LinearLayout) findViewById(R.id.ll_hdm);
    }

    @Override
    public void show(View fromView) {
        tvCount.setText(Integer.toString(AddressManager.getInstance().getPrivKeyAddresses().size()));
        if (AddressManager.getInstance().hasHDMKeychain()) {
            llHDM.setVisibility(View.VISIBLE);
        } else {
            llHDM.setVisibility(View.GONE);
        }
        super.show(fromView);
    }
}
