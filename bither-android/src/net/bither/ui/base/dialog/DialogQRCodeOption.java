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

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import net.bither.R;

public class DialogQRCodeOption extends CenterDialog {
    public interface ISwitchQRCode {
        public void switchQRCode();
    }

    private TextView tvSwitch;
    private TextView tvClose;
    private Activity activity;
    private ISwitchQRCode switchQRCode;


    public DialogQRCodeOption(Activity context,
                              ISwitchQRCode switchQRCode) {
        super(context);
        this.activity = context;
        this.switchQRCode = switchQRCode;
        setContentView(R.layout.dialog_qrcode_option);
        tvSwitch = (TextView) findViewById(R.id.tv_switch_qrcode);
        tvClose = (TextView) findViewById(R.id.tv_close);
        tvSwitch.setOnClickListener(viewOnBlockchainInfoClick);
        tvClose.setOnClickListener(closeClick);

    }

    @Override
    public void show() {
        super.show();
    }

    private View.OnClickListener viewOnBlockchainInfoClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (switchQRCode != null) {
                switchQRCode.switchQRCode();
            }
            dismiss();

        }
    };

    private View.OnClickListener closeClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dismiss();
        }
    };
}
