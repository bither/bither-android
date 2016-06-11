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
import android.view.View;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.BitherjSettings;
import net.bither.preference.AppSharedPreference;
import net.bither.util.WalletUtils;

/**
 * Created by songchenwen on 16/6/11.
 */
public class DialogHDMonitorFirstAddressValidation extends CenterDialog {
    private TextView tvTitle;
    private TextView tvAddress;

    public DialogHDMonitorFirstAddressValidation(Context context, String address) {
        super(context);
        setContentView(R.layout.dialog_hd_monitor_first_address_validation);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        findViewById(R.id.btn_ok).setOnClickListener(okClicked);
        if (AppSharedPreference.getInstance().getAppMode() == BitherjSettings.AppMode.COLD) {
            tvTitle.setVisibility(View.GONE);
        } else {
            tvTitle.setVisibility(View.VISIBLE);
        }
        tvAddress.setText(WalletUtils.formatHash(address, 4, 16));
    }

    private View.OnClickListener okClicked = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dismiss();
        }
    };
}
