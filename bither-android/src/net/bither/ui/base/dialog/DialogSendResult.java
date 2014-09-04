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
import android.widget.Button;
import android.widget.TextView;

import net.bither.R;
import net.bither.util.GenericUtils;

import java.math.BigInteger;

public class DialogSendResult extends CenterDialog {

    public DialogSendResult(Context context, String address, BigInteger btc,
                            BigInteger fee) {
        super(context);
        setContentView(R.layout.dialog_send_result);
        TextView tvAddress = (TextView) findViewById(R.id.tv_address);
        TextView tvBtc = (TextView) findViewById(R.id.tv_btc);
        TextView tvFee = (TextView) findViewById(R.id.tv_fee);
        Button btnClose = (Button) findViewById(R.id.btn_close);
        tvAddress.setText(address);
        tvBtc.setText(GenericUtils.formatValueWithBold(btc.longValue()));
        tvFee.setText(GenericUtils.formatValueWithBold(fee.longValue()));
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
